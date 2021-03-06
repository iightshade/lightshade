package com.logcat.example.pc.orbitalproj;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.util.ArrayList;


public class MedicationViewer extends AppCompatActivity{

    TextView medicationTitleText;
    TextView medicationDescriptionText;
    ImageView medicationImage;
    TextView medicationQuantityText;
    TextView medicationSpinnerText;
    TextView timeText;
    TextView daysText;
    TextView startDateText;
    TextView endDateText;
    Button editButton;
    Button returnButton;
    Intent intent;
    Medication medication;

    private FirebaseAuth userAuth;
    private StorageReference storageReference;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme2);
        setContentView(R.layout.medication_viewer_layout);
        getSupportActionBar().setTitle("Medication");

        userAuth = FirebaseAuth.getInstance();
        userId = userAuth.getCurrentUser().getUid();
        storageReference = FirebaseStorage.getInstance().getReference(userId);

        medicationTitleText = (TextView)findViewById(R.id.medicationTitleText);
        medicationDescriptionText = (TextView)findViewById(R.id.medicationDescriptionText);
        medicationImage = (ImageView) findViewById(R.id.medicationImage) ;
        medicationQuantityText = (TextView) findViewById(R.id.medicationQuantityText);
        medicationSpinnerText = (TextView) findViewById(R.id.medicationSpinnerText);
        timeText = (TextView) findViewById(R.id.timeText);
        daysText = (TextView) findViewById(R.id.daysText);
        startDateText = (TextView) findViewById(R.id.startDateText);
        endDateText = (TextView) findViewById(R.id.endDateText);

        editButton = (Button)findViewById(R.id.editButton);
        returnButton = (Button)findViewById(R.id.returnButton);

        intent = getIntent();
        medication = intent.getParcelableExtra("Medication");

        medicationTitleText.setText(medication.getMedicationTitle());
        medicationDescriptionText.setText(medication.getMedicationDescription());

        String medicationId = medication.getMedicationId();
        StorageReference importReference = storageReference.child(medicationId + ".jpg");

        importReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri downloadUrl) {
                Glide.with(MedicationViewer.this)
                        .load(downloadUrl)
                        .dontAnimate()
                        .into(medicationImage);
            }
        });

        medicationQuantityText.setText(medication.getMedicationQuantity());
        switch(medication.getMedicationServingPosition()){
            case(0):
                medicationSpinnerText.setText("ml");
                break;
            case(1):
                medicationSpinnerText.setText("Tablet");
                break;
            case(2):
                medicationSpinnerText.setText("Apply");
                break;
            case(3):
                medicationSpinnerText.setText("Teaspoon");
                break;
            case(4):
                medicationSpinnerText.setText("Tablespoon");
                break;
            default:
                medicationSpinnerText.setText("Serving");
                break;
        }

        int numHours = Integer.parseInt(medication.getMedicationHour());
        int numMinutes = Integer.parseInt(medication.getMedicationMinute());
        setTimeText(numHours, numMinutes);
        ArrayList<Boolean> medicationDays;
        medicationDays = medication.getMedicationDays();
        boolean[] daysChecked = new boolean[medicationDays.size()];
        for(int i = 0; i < medicationDays.size(); i++)
        {
            daysChecked[i] = medicationDays.get(i);
        }
        setDaysText(daysChecked);

        Integer startYear, startMonth, startDay;
        Integer endYear, endMonth, endDay;
        startYear = medication.getMedicationStartYear();
        startMonth = medication.getMedicationStartMonth();
        startDay = medication.getMedicationStartDay();
        endYear = medication.getMedicationEndYear();
        endMonth = medication.getMedicationEndMonth();
        endDay = medication.getMedicationEndDay();
        startDateText.setText(setDatesTextView(startYear, startMonth, startDay));
        endDateText.setText(setDatesTextView(endYear, endMonth, endDay));


        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent2 = getIntent();
                String activity = intent2.getStringExtra("Medicine");

                intent = new Intent(MedicationViewer.this , MedicationEdit.class);
                intent.putExtra("Medication", medication);

                if (activity.equals("MedicationFragment")){
                    intent.putExtra("Medicine", "MedicationFragment");
                } else if (activity.equals("CalendarFragment3Day")){
                    intent.putExtra("Medicine", "CalendarFragment3Day");
                } else if (activity.equals("CalendarFragmentDay")){
                    intent.putExtra("Medicine", "CalendarFragmentDay");
                } else if(activity.equals("CalendarFragmentWeek")){
                    intent.putExtra("Medicine", "CalendarFragmentWeek");
                }
                startActivity(intent);
                finish();

            }
        });

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // use finish() only if you want to use it as a back function
            }
        });

    }

    private void setTimeText(int numHour, int numMinute) {

        Boolean isMorning = true;
        String hourString;
        String minuteString;

        if (numHour >= 12) {
            isMorning = false;
        }

        if (numHour > 12) {
            numHour = (numHour - 12);
        }

        if(numHour == 0){
            hourString = "12";
        } else if (numHour < 10) {
            hourString = "0" + Integer.toString(numHour);
        } else {
            hourString = Integer.toString(numHour);
        }

        if (numMinute < 10) {
            minuteString = "0" + Integer.toString(numMinute);
        } else {
            minuteString = Integer.toString(numMinute);
        }

        timeText.setText(hourString + ":" + minuteString);

        if (isMorning == true) {
            timeText.append(" AM");
        } else {
            timeText.append(" PM");
        }
    }

    private void setDaysText(boolean[] dates){
        daysText.setText("");
        for(int i =0; i<7; i++){
            if(dates[i] == true){
                switch (i){
                    case 0:daysText.append("Mon ");    continue;
                    case 1:daysText.append("Tues ");   continue;
                    case 2:daysText.append("Wed ");    continue;
                    case 3:daysText.append("Thurs ");  continue;
                    case 4:daysText.append("Fri ");    continue;
                    case 5:daysText.append("Sat ");    continue;
                    case 6:daysText.append("Sun ");    continue;
                }
            }
        }
        if(daysText.getText().toString().equals("")){
            daysText.setText("Enter dates here.");
        }
    }

    private String setDatesTextView(int year, int month, int day){
        String date = "";
        date = Integer.toString(day) + " ";
        switch (month) {
            case 0:
                date = date + "Jan ";
                break;
            case 1:
                date = date + "Feb ";
                break;
            case 2:
                date = date + "Mar ";
                break;
            case 3:
                date = date + "Apr ";
                break;
            case 4:
                date = date + "May ";
                break;
            case 5:
                date = date + "Jun ";
                break;
            case 6:
                date = date + "Jul ";
                break;
            case 7:
                date = date + "Aug ";
                break;
            case 8:
                date = date + "Sep ";
                break;
            case 9:
                date = date + "Oct ";
                break;
            case 10:
                date = date + "Nov ";
                break;
            case 11:
                date = date + "Dec ";
                break;
        }
        date = date + Integer.toString(year);

        return date;
    }

}
