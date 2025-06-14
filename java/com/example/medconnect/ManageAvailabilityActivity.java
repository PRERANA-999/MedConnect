package com.example.medconnect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ManageAvailabilityActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateTextView;
    private Button buttonSaveAvailability;
    private RecyclerView recyclerViewTimeSlots;
    private ProgressBar progressBarAvailability;

    private CheckBox checkboxInClinic, checkboxVideoConsultation;

    private Button buttonSlot900, buttonSlot930, buttonSlot1000, buttonSlot1030,
            buttonSlot1100, buttonSlot1130, buttonSlot1200, buttonSlot1230,
            buttonSlot1300, buttonSlot1330,
            buttonSlot1400, buttonSlot1430, buttonSlot1500, buttonSlot1530,
            buttonSlot1600, buttonSlot1630, buttonSlot1700, buttonSlot1730,
            buttonSlot1800, buttonSlot1830, buttonSlot1900, buttonSlot1930,
            buttonSlot2000, buttonSlot2030;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private String selectedDate;
    private SimpleDateFormat sdfDateOnly;
    private SimpleDateFormat sdfDateTime;

    private ArrayList<Map<String, String>> timeSlots = new ArrayList<>();
    private DoctorAvailabilityAdapter doctorAvailabilityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_availability);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Availability");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Doctor not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        calendarView = findViewById(R.id.calendarView);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        buttonSaveAvailability = findViewById(R.id.buttonSaveAvailability);
        recyclerViewTimeSlots = findViewById(R.id.recyclerViewTimeSlots);
        progressBarAvailability = findViewById(R.id.progressBarAvailability);

        checkboxInClinic = findViewById(R.id.checkboxInClinic);
        checkboxVideoConsultation = findViewById(R.id.checkboxVideoConsultation);

        buttonSlot900 = findViewById(R.id.buttonSlot900);
        buttonSlot930 = findViewById(R.id.buttonSlot930);
        buttonSlot1000 = findViewById(R.id.buttonSlot1000);
        buttonSlot1030 = findViewById(R.id.buttonSlot1030);
        buttonSlot1100 = findViewById(R.id.buttonSlot1100);
        buttonSlot1130 = findViewById(R.id.buttonSlot1130);
        buttonSlot1200 = findViewById(R.id.buttonSlot1200);
        buttonSlot1230 = findViewById(R.id.buttonSlot1230);
        buttonSlot1300 = findViewById(R.id.buttonSlot1300);
        buttonSlot1330 = findViewById(R.id.buttonSlot1330);
        buttonSlot1400 = findViewById(R.id.buttonSlot1400);
        buttonSlot1430 = findViewById(R.id.buttonSlot1430);
        buttonSlot1500 = findViewById(R.id.buttonSlot1500);
        buttonSlot1530 = findViewById(R.id.buttonSlot1530);
        buttonSlot1600 = findViewById(R.id.buttonSlot1600);
        buttonSlot1630 = findViewById(R.id.buttonSlot1630);
        buttonSlot1700 = findViewById(R.id.buttonSlot1700);
        buttonSlot1730 = findViewById(R.id.buttonSlot1730);
        buttonSlot1800 = findViewById(R.id.buttonSlot1800);
        buttonSlot1830 = findViewById(R.id.buttonSlot1830);
        buttonSlot1900 = findViewById(R.id.buttonSlot1900);
        buttonSlot1930 = findViewById(R.id.buttonSlot1930);
        buttonSlot2000 = findViewById(R.id.buttonSlot2000);
        buttonSlot2030 = findViewById(R.id.buttonSlot2030);


        recyclerViewTimeSlots.setLayoutManager(new LinearLayoutManager(this));
        doctorAvailabilityAdapter = new DoctorAvailabilityAdapter(timeSlots);
        recyclerViewTimeSlots.setAdapter(doctorAvailabilityAdapter);

        sdfDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        Calendar todayCalendar = Calendar.getInstance();
        calendarView.setMinDate(todayCalendar.getTimeInMillis());
        selectedDate = sdfDateOnly.format(todayCalendar.getTime());
        selectedDateTextView.setText("Selected Date: " + selectedDate);
        loadTimeSlotsForSelectedDate();

        checkboxInClinic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkboxVideoConsultation.setChecked(false);
            }
        });
        checkboxVideoConsultation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkboxInClinic.setChecked(false);
            }
        });


        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(year, month, dayOfMonth, 0, 0, 0);

            Calendar todayCal = Calendar.getInstance();
            todayCal.set(todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

            if (selectedCal.before(todayCal)) {
                Toast.makeText(this, "Cannot select a past date.", Toast.LENGTH_SHORT).show();
                calendarView.setDate(System.currentTimeMillis(), true, true);
                selectedDate = sdfDateOnly.format(todayCal.getTime());
                selectedDateTextView.setText("Selected Date: " + selectedDate);
                loadTimeSlotsForSelectedDate();
                return;
            }

            selectedCal.set(year, month, dayOfMonth);
            selectedDate = sdfDateOnly.format(selectedCal.getTime());
            selectedDateTextView.setText("Selected Date: " + selectedDate);
            loadTimeSlotsForSelectedDate();
        });

        buttonSlot900.setOnClickListener(v -> addPredefinedTimeSlot("09:00-09:30"));
        buttonSlot930.setOnClickListener(v -> addPredefinedTimeSlot("09:30-10:00"));
        buttonSlot1000.setOnClickListener(v -> addPredefinedTimeSlot("10:00-10:30"));
        buttonSlot1030.setOnClickListener(v -> addPredefinedTimeSlot("10:30-11:00"));
        buttonSlot1100.setOnClickListener(v -> addPredefinedTimeSlot("11:00-11:30"));
        buttonSlot1130.setOnClickListener(v -> addPredefinedTimeSlot("11:30-12:00"));
        buttonSlot1200.setOnClickListener(v -> addPredefinedTimeSlot("12:00-12:30"));
        buttonSlot1230.setOnClickListener(v -> addPredefinedTimeSlot("12:30-13:00"));
        buttonSlot1300.setOnClickListener(v -> addPredefinedTimeSlot("13:00-13:30"));
        buttonSlot1330.setOnClickListener(v -> addPredefinedTimeSlot("13:30-14:00"));

        buttonSlot1400.setOnClickListener(v -> addPredefinedTimeSlot("14:00-14:30"));
        buttonSlot1430.setOnClickListener(v -> addPredefinedTimeSlot("14:30-15:00"));
        buttonSlot1500.setOnClickListener(v -> addPredefinedTimeSlot("15:00-15:30"));
        buttonSlot1530.setOnClickListener(v -> addPredefinedTimeSlot("15:30-16:00"));
        buttonSlot1600.setOnClickListener(v -> addPredefinedTimeSlot("16:00-16:30"));
        buttonSlot1630.setOnClickListener(v -> addPredefinedTimeSlot("16:30-17:00"));
        buttonSlot1700.setOnClickListener(v -> addPredefinedTimeSlot("17:00-17:30"));
        buttonSlot1730.setOnClickListener(v -> addPredefinedTimeSlot("17:30-18:00"));
        buttonSlot1800.setOnClickListener(v -> addPredefinedTimeSlot("18:00-18:30"));
        buttonSlot1830.setOnClickListener(v -> addPredefinedTimeSlot("18:30-19:00"));
        buttonSlot1900.setOnClickListener(v -> addPredefinedTimeSlot("19:00-19:30"));
        buttonSlot1930.setOnClickListener(v -> addPredefinedTimeSlot("19:30-20:00"));
        buttonSlot2000.setOnClickListener(v -> addPredefinedTimeSlot("20:00-20:30"));
        buttonSlot2030.setOnClickListener(v -> addPredefinedTimeSlot("20:30-21:00"));


        buttonSaveAvailability.setOnClickListener(v -> saveAvailability());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addPredefinedTimeSlot(String timeRange) {
        if (selectedDate == null || selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String slotType;
        if (checkboxInClinic.isChecked()) {
            slotType = "In-clinic";
        } else if (checkboxVideoConsultation.isChecked()) {
            slotType = "Video Consultation";
        } else {
            Toast.makeText(this, "Please select an appointment type (In-clinic or Video Consultation).", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            String startTimeStr = timeRange.split("-")[0];
            Date selectedSlotStartDateTime = sdfDateTime.parse(selectedDate + " " + startTimeStr);
            Date currentDateTime = Calendar.getInstance().getTime();

            if (selectedSlotStartDateTime.before(currentDateTime)) {
                Toast.makeText(this, "Cannot add a time slot in the past. Please select a future time.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Error parsing time slot. Please try again.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }

        for (Map<String, String> existingSlot : timeSlots) {
            String existingTimeRange = existingSlot.get("time");
            if (existingTimeRange != null && existingTimeRange.equals(timeRange)) {
                Toast.makeText(this, "This time slot is already added.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Map<String, String> newSlot = new HashMap<>();
        newSlot.put("time", timeRange);
        newSlot.put("type", slotType);
        newSlot.put("isBooked", "false");

        timeSlots.add(newSlot);
        sortTimeSlots();
        doctorAvailabilityAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Slot " + timeRange + " (" + slotType + ") added. Click 'Save All Slots'.", Toast.LENGTH_LONG).show();
    }


    private void sortTimeSlots() {
        Collections.sort(timeSlots, (s1, s2) -> {
            String time1 = s1.get("time");
            String time2 = s2.get("time");
            if (time1 == null || time2 == null) return 0;
            String startTime1 = time1.split("-")[0];
            String startTime2 = time2.split("-")[0];
            return startTime1.compareTo(startTime2);
        });
    }

    private void loadTimeSlotsForSelectedDate() {
        progressBarAvailability.setVisibility(View.VISIBLE);
        db.collection("users").document(currentUser.getUid())
                .collection("availability").document(selectedDate)
                .get()
                .addOnCompleteListener(task -> {
                    progressBarAvailability.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        timeSlots.clear();
                        if (document.exists() && document.contains("slots")) {
                            ArrayList<Map<String, Object>> loadedSlots = (ArrayList<Map<String, Object>>) document.get("slots");
                            if (loadedSlots != null) {
                                for(Map<String, Object> slotMap : loadedSlots) {
                                    Map<String, String> currentSlot = new HashMap<>();
                                    currentSlot.put("time", (String) slotMap.get("time"));
                                    currentSlot.put("type", (String) slotMap.get("type"));
                                    Object isBookedValue = slotMap.get("isBooked");
                                    if (isBookedValue instanceof Boolean) {
                                        currentSlot.put("isBooked", String.valueOf((Boolean) isBookedValue));
                                    } else if (isBookedValue instanceof String) {
                                        currentSlot.put("isBooked", (String) isBookedValue);
                                    } else {
                                        currentSlot.put("isBooked", "false");
                                    }
                                    timeSlots.add(currentSlot);
                                }
                                sortTimeSlots();
                            }
                            Toast.makeText(ManageAvailabilityActivity.this, "Slots loaded for " + selectedDate, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ManageAvailabilityActivity.this, "No slots for " + selectedDate, Toast.LENGTH_SHORT).show();
                        }
                        doctorAvailabilityAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(ManageAvailabilityActivity.this, "Error loading slots: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveAvailability() {
        if (selectedDate == null || selectedDate.isEmpty()) {
            Toast.makeText(this, "No date selected to save slots for.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarAvailability.setVisibility(View.VISIBLE);

        if (timeSlots.isEmpty()) {
            db.collection("users").document(currentUser.getUid())
                    .collection("availability").document(selectedDate)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        progressBarAvailability.setVisibility(View.GONE);
                        Toast.makeText(ManageAvailabilityActivity.this, "All slots removed for " + selectedDate, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressBarAvailability.setVisibility(View.GONE);
                        Toast.makeText(ManageAvailabilityActivity.this, "Failed to remove slots: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            return;
        }

        Map<String, Object> data = new HashMap<>();
        ArrayList<Map<String, Object>> slotsForFirestore = new ArrayList<>();
        for (Map<String, String> slot : timeSlots) {
            Map<String, Object> firestoreSlot = new HashMap<>(slot);
            firestoreSlot.put("isBooked", Boolean.parseBoolean(slot.get("isBooked")));
            slotsForFirestore.add(firestoreSlot);
        }
        data.put("slots", slotsForFirestore);


        db.collection("users").document(currentUser.getUid())
                .collection("availability").document(selectedDate)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBarAvailability.setVisibility(View.GONE);
                    Toast.makeText(ManageAvailabilityActivity.this, "Availability saved for " + selectedDate, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBarAvailability.setVisibility(View.GONE);
                    Toast.makeText(ManageAvailabilityActivity.this, "Failed to save availability: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private class DoctorAvailabilityAdapter extends RecyclerView.Adapter<DoctorAvailabilityAdapter.ViewHolder> {
        private ArrayList<Map<String, String>> localTimeSlots;

        public DoctorAvailabilityAdapter(ArrayList<Map<String, String>> timeSlots) {
            this.localTimeSlots = timeSlots;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_availability_slot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> slot = localTimeSlots.get(position);
            String timeRange = slot.get("time");
            String type = slot.get("type");

            holder.timeSlotTextView.setText(timeRange);
            holder.slotTypeTextView.setText("(" + type + ")");

            holder.buttonRemoveSlot.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    localTimeSlots.remove(adapterPosition);
                    notifyItemRemoved(adapterPosition);
                    Toast.makeText(ManageAvailabilityActivity.this, "Slot removed from list. Click 'Save All Slots' to confirm.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return localTimeSlots.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView timeSlotTextView;
            TextView slotTypeTextView;
            Button buttonRemoveSlot;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                timeSlotTextView = itemView.findViewById(R.id.timeSlotTextView);
                slotTypeTextView = itemView.findViewById(R.id.slotTypeTextView);
                buttonRemoveSlot = itemView.findViewById(R.id.buttonRemoveSlot);
            }
        }
    }
}