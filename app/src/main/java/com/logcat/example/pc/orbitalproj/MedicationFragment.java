package com.logcat.example.pc.orbitalproj;


import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;


public class MedicationFragment extends Fragment {

    FirebaseAuth userAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    FirebaseUser firebaseUser;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference userReference;
    StorageReference storageReference;

    String userId;
    Medication medication;
    Medication temp;
    ArrayList<Medication> tempList;
    Calendar calendar, upcoming;
    boolean arranging;
    int positionX;

    View view;
    Button medicationFragmentAddButton;

    GridLayoutManager gridLayoutManager;
    RecyclerView medicationRecyclerView;
    MedicationViewRecyclerAdapter medicationViewRecyclerAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = userAuth.getCurrentUser();

        userId = firebaseUser.getUid();
        userReference = firebaseDatabase.getReference(userId);
        storageReference = FirebaseStorage.getInstance().getReference(userId);

        arranging = false;

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_medication, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.medicationFragmentToolBar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("Medication");

        medicationRecyclerView = (RecyclerView) view.findViewById(R.id.medicationRecyclerView);
        gridLayoutManager = new GridLayoutManager(getActivity(), 2);
        medicationRecyclerView.setLayoutManager(gridLayoutManager);
        medicationFragmentAddButton = (Button) view.findViewById(R.id.medicationFragmentAddButton);
        calendar = Calendar.getInstance();
        upcoming = Calendar.getInstance();


        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                tempList = new ArrayList<Medication>();
                tempList.clear();
                int keyCounter = 0;

                for (DataSnapshot categoriesSnapShot : dataSnapshot.getChildren()) {
                    temp = categoriesSnapShot.getValue(Medication.class);
                    tempList.add(temp);

                    Calendar upcoming = upcomingMedication(temp);

                    if (upcoming != null) {

                        Long alarmTime = upcoming.getTimeInMillis();

                        Intent intent = new Intent(getActivity(), NotificationBroadcast.class);
                        intent.putExtra("Key", keyCounter);
                        intent.putExtra("medicationTitle", temp.getMedicationTitle());

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), keyCounter, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);

                        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    }

                    keyCounter++;
                }

                medicationViewRecyclerAdapter = new MedicationViewRecyclerAdapter(getActivity(), tempList);
                medicationRecyclerView.setAdapter(medicationViewRecyclerAdapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        medicationRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), medicationRecyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (arranging == true) {
                            arranging = false;
                            userReference.child(tempList.get(positionX).getMedicationId()).setPriority(position);
                            userReference.child(tempList.get(position).getMedicationId()).setPriority(positionX);
                            medicationFragmentAddButton.setOnClickListener(null);
                            medicationFragmentAddButton.setText("Add Medication");
                            setMedicationFragmentAddButton();

                        } else if (arranging == false) {
                            Intent intent = new Intent(getActivity(), MedicationViewer.class);
                            medication = tempList.get(position);
                            intent.putExtra("Medication", medication);
                            intent.putExtra("Medicine", "MedicationFragment");
                            startActivity(intent);
                        }

                    }

                    @Override
                    public void onItemLongClick(View view, final int position) {
                        if (arranging == true) {
                            return;
                        }
                        final PopupMenu popupMenu = new PopupMenu(getContext(), view);
                        popupMenu.getMenuInflater().inflate(R.menu.medication_popup, popupMenu.getMenu());

                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                if (item.getTitle().toString().equals("Delete")) {
                                    ConfirmDelete(position);
                                    popupMenu.dismiss();
                                }

                                if (item.getTitle().toString().equals("Rearrange")) {
                                    arranging = true;
                                    positionX = position;
                                    medicationFragmentAddButton.setOnClickListener(null);
                                    medicationFragmentAddButton.setText("Cancel swap");

                                    medicationFragmentAddButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            arranging = false;
                                            medicationFragmentAddButton.setOnClickListener(null);
                                            medicationFragmentAddButton.setText("Add Medication");
                                            setMedicationFragmentAddButton();

                                        }
                                    });
                                    popupMenu.dismiss();

                                }

                                if (item.getTitle().toString().equals("Edit")) {
                                    Intent intent = new Intent(getActivity(), MedicationEdit.class);
                                    medication = tempList.get(position);
                                    intent.putExtra("Medication", medication);
                                    intent.putExtra("Medicine", "MedicationFragment");
                                    startActivity(intent);
                                    popupMenu.dismiss();
                                }

                                return false;
                            }
                        });

                        popupMenu.show();
                    }
                }));

        setMedicationFragmentAddButton();

        try {
            final String notifiedMedication = getArguments().getString("notifiedMedication");
            final int key = (getArguments().getInt("Key") * -1);
            if(notifiedMedication != null){
                AlertDialog.Builder notifBuilder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AppTheme2));
                notifBuilder.setCancelable(false);
                notifBuilder.setTitle("Have you taken " + notifiedMedication + "?");
                notifBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                notifBuilder.setNeutralButton("Remind me in 5mins", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Calendar calendar = Calendar.getInstance();
                        Long calendarAdded = calendar.getTimeInMillis() + 1000 * 60;
                        calendar.setTimeInMillis(calendarAdded);

                        Intent intent = new Intent(getActivity(), NotificationBroadcast.class);
                        intent.putExtra("Key", key);
                        intent.putExtra("medicationTitle", notifiedMedication);

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), key, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                        dialog.dismiss();
                    }
                });
                notifBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                Dialog notifDialog = notifBuilder.show();

            }

        } catch (NullPointerException e) {
            String notifiedMedication = null;
        }

        return view;

    }

    @Override
    public void onStart() {
        super.onStart();


        /*medicationGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getActivity(), MedicationViewer.class);
                medication = tempList.get(position);
                intent.putExtra("Medication", medication);
                intent.putExtra("Medicine", "MedicationFragment");
                startActivity(intent);

            }
        });

        medicationGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                final PopupMenu popupMenu = new PopupMenu(getContext(), view);
                popupMenu.getMenuInflater().inflate(R.menu.medication_popup, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getTitle().toString().equals("Delete")) {
                            ConfirmDelete(position);
                            popupMenu.dismiss();
                        }

                        if (item.getTitle().toString().equals("Rearrange")) {
                            rearrangePriorities(position);
                            popupMenu.dismiss();
                        }

                        if (item.getTitle().toString().equals("Edit")) {
                            Intent intent = new Intent(getActivity(), MedicationEdit.class);
                            medication = tempList.get(position);
                            intent.putExtra("Medication", medication);
                            intent.putExtra("Medicine", "MedicationFragment");
                            startActivity(intent);
                            popupMenu.dismiss();
                        }

                        return true;
                    }
                });

                popupMenu.show();
                return true;
            }
        });
*/

    }

    private boolean ConfirmDelete(Integer position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AppTheme2));
        builder.setTitle("Delete medication?");
        final String dialogMessage = tempList.get(position).getMedicationTitle();
        final String medicationId = tempList.get(position).getMedicationId();
        builder.setMessage(dialogMessage);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {

                final ProgressDialog progressDialog = new ProgressDialog(getActivity());
                progressDialog.setMessage("Loading");
                progressDialog.show();

                userReference.child(medicationId).removeValue();
                storageReference.child(medicationId + ".jpg").delete().
                        addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(getActivity(), "Medication deleted", Toast.LENGTH_SHORT).show();
                            }
                        }).
                        addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressDialog.dismiss();
                                Toast.makeText(getActivity(), "Medication deleted", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                dialog.dismiss();
            }
        });

        builder.show();

        return true;

    }

    private Calendar upcomingMedication(Medication medication) {
        //returns a 'Calendar' that contains the medication's most upcoming date and time of consumption

        if ((checkStartAndEndDates(medication)) == false) {
            return null;
        }
        if (checkTiming(medication) == false) {
            return null;
        }

        ArrayList<Boolean> daysChecked;
        Calendar calendar;

        Integer medicationHour = 0;
        Integer medicationMinute = 0;
        calendar = Calendar.getInstance();

        int today = calendar.get(Calendar.DAY_OF_WEEK);

        daysChecked = medication.getMedicationDays();

        //convert to database version
        int alarmDay = today - 2;
        if (alarmDay < 0) {
            alarmDay = 6;
        }

        for (int counter = 0; counter < 7; counter++) {
            if (daysChecked.get(alarmDay) == true) {
                medicationHour = Integer.valueOf(medication.getMedicationHour());
                medicationMinute = Integer.valueOf(medication.getMedicationMinute());
                break;
            } else {
                //go to the next day. check if next date is out of bounds
                alarmDay++;
                if (alarmDay > 6) {
                    alarmDay = 0;
                }
            }
        }
        //convert back to android version
        alarmDay = alarmDay + 2;
        if (alarmDay > 7) {
            alarmDay = 1;
        }

        calendar.set(Calendar.DAY_OF_WEEK, alarmDay);
        calendar.set(Calendar.HOUR_OF_DAY, medicationHour);
        calendar.set(Calendar.MINUTE, medicationMinute);
        calendar.set(Calendar.SECOND, 0);

        return calendar;
    }

    private boolean checkStartAndEndDates(Medication medication) {
        //returns true if today's date is within start and end date limits
        Integer startYear = medication.getMedicationStartYear();
        Integer startMonth = medication.getMedicationStartMonth();
        Integer startDay = medication.getMedicationStartDay();
        Integer startDate = startYear * 10000 + startMonth * 100 + startDay;

        Integer endYear = medication.getMedicationEndYear();
        Integer endMonth = medication.getMedicationEndMonth();
        Integer endDay = medication.getMedicationEndDay();
        Integer endDate = endYear * 10000 + endMonth * 100 + endDay;

        Calendar calendar = Calendar.getInstance();
        Integer currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        Integer currentMonth = calendar.get(Calendar.MONTH);
        Integer currentYear = calendar.get(Calendar.YEAR);
        Integer currentDate = currentYear * 10000 + currentMonth * 100 + currentDay;

        if ((startDate <= currentDate) && (currentDate <= endDate)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkTiming(Medication medication) {
        // Returns true if current time is before medication time
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int medicationHour = Integer.parseInt(medication.getMedicationHour());
        int medicationMinute = Integer.parseInt(medication.getMedicationMinute());

        if ((currentHour * 60 + currentMinute) < (medicationHour * 60 + medicationMinute)) {
            return true;
        } else {
            return false;
        }
    }

    private void setMedicationFragmentAddButton() {
        medicationFragmentAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MedicationEdit.class);
                intent.putExtra("Medicine", "MedicationFragment");
                startActivity(intent);
            }
        });
    }

}














