package com.example.medconnect; // Ensure this matches your package name

import android.content.Intent;
import android.graphics.Color; // For button color
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button; // Standard Button
import android.widget.CalendarView;
import android.widget.CheckBox; // For appointment type selection
import android.widget.GridLayout; // For time slots
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton; // For time slot buttons
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminManageDoctorAvailabilityActivity extends AppCompatActivity {

    private static final String TAG = "AdminAvailability";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String doctorUidToManage;
    private String doctorNameToManage;

    private CalendarView calendarView;
    private TextView selectedDateTextView;

    // --- UPDATED UI ELEMENTS FROM YOUR XML ---
    private CheckBox checkboxInClinic;
    private CheckBox checkboxVideoConsultation;
    private GridLayout gridLayoutTimeSlots; // This replaces ChipGroup
    private MaterialButton buttonSaveAvailability; // Corrected Save button ID
    private RecyclerView recyclerViewTimeSlots; // Corrected RecyclerView ID
    // --- END UPDATED UI ELEMENTS ---

    private SelectedSlotsAdapter selectedSlotsAdapter;
    private List<String> currentSelectedSlots; // List to hold slots selected for the current date and type

    private SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private String currentSelectedDate; // Stores the yyyy-MM-dd string
    private String currentAppointmentType = "In-clinic"; // Default type

    // Map to store references to MaterialButtons for easy access and state management
    private Map<String, MaterialButton> timeSlotButtons = new HashMap<>();

    // Map to keep track of the selected state for each time slot (per date & type)
    private Set<String> currentlySelectedSlotsSet = new HashSet<>();


    // All possible time slots (based on your GridLayout buttons)
    private static final String[] ALL_TIME_SLOTS = {
            "09:00-09:15", "09:15-09:30", "09:30-09:45", "09:45-10:00",
            "10:00-10:15", "10:15-10:30", "10:30-10:45", "10:45-11:00",
            "11:00-11:15", "11:15-11:30", "11:30-11:45", "11:45-12:00"
            // Your XML only defines up to 11:15-11:30 (buttonSlot1330 is 11:15-11:30)
            // You might need to extend your GridLayout and this array if you have more slots in reality
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_availability);

        mAuth = FirebaseAuth.getInstance(); // Ensure mAuth is initialized
        db = FirebaseFirestore.getInstance(); // Ensure db is initialized

        Intent intent = getIntent();
        doctorUidToManage = intent.getStringExtra("doctorUidToManage");
        doctorNameToManage = intent.getStringExtra("doctorName");

        // *************** ADD THESE LOG STATEMENTS ***************
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "Current Logged-in User UID (Admin): " + mAuth.getCurrentUser().getUid());
        } else {
            Log.e(TAG, "No user logged in to AdminManageDoctorAvailabilityActivity!");
        }
        Log.d(TAG, "Doctor UID being managed: " + doctorUidToManage);
        Log.d(TAG, "Attempting to manage date: " + currentSelectedDate); // This will be the initial date
        // **********************************************************

        if (doctorUidToManage == null || doctorUidToManage.isEmpty()) {
            Toast.makeText(this, "Error: No doctor selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // Set up ActionBar title dynamically
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Avail. for " + doctorNameToManage);
        }
        // --- END NEW ---

        // Initialize UI components matching your XML
        calendarView = findViewById(R.id.calendarView);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        checkboxInClinic = findViewById(R.id.checkboxInClinic);
        checkboxVideoConsultation = findViewById(R.id.checkboxVideoConsultation);
        //gridLayoutTimeSlots = findViewById(R.id.gridLayoutTimeSlots); // Initialize GridLayout
        buttonSaveAvailability = findViewById(R.id.buttonSaveAvailability); // Initialize save button
        recyclerViewTimeSlots = findViewById(R.id.recyclerViewTimeSlots); // Initialize RecyclerView

        // Initialize MaterialButtons within the GridLayout (you'll need to map them manually)
        timeSlotButtons.put("09:00-09:15", findViewById(R.id.buttonSlot900));
        timeSlotButtons.put("09:15-09:30", findViewById(R.id.buttonSlot930));
        timeSlotButtons.put("09:30-09:45", findViewById(R.id.buttonSlot1000)); // Be careful, ID names don't match text
        timeSlotButtons.put("09:45-10:00", findViewById(R.id.buttonSlot1030));
        timeSlotButtons.put("10:00-10:15", findViewById(R.id.buttonSlot1100));
        timeSlotButtons.put("10:15-10:30", findViewById(R.id.buttonSlot1130));
        timeSlotButtons.put("10:30-10:45", findViewById(R.id.buttonSlot1200));
        timeSlotButtons.put("10:45-11:00", findViewById(R.id.buttonSlot1230));
        timeSlotButtons.put("11:00-11:15", findViewById(R.id.buttonSlot1300));
        timeSlotButtons.put("11:15-11:30", findViewById(R.id.buttonSlot1330));
        // Add more if you have more buttons in your GridLayout

        // Set up click listeners for the time slot buttons
        for (Map.Entry<String, MaterialButton> entry : timeSlotButtons.entrySet()) {
            String slot = entry.getKey();
            MaterialButton button = entry.getValue();
            button.setOnClickListener(v -> toggleSlotSelection(slot, button));
        }


        // Set up RecyclerView
        recyclerViewTimeSlots.setLayoutManager(new LinearLayoutManager(this));
        currentSelectedSlots = new ArrayList<>();
        // Make sure item_doctor_availability_slot.xml exists or use item_selected_slot.xml
        selectedSlotsAdapter = new SelectedSlotsAdapter(currentSelectedSlots, new SelectedSlotsAdapter.OnSlotActionListener() {
            @Override
            public void onRemoveClick(String slot) {
                // When a slot is removed from the RecyclerView, deselect its button
                if (currentlySelectedSlotsSet.contains(slot)) {
                    currentlySelectedSlotsSet.remove(slot);
                    // Find the corresponding MaterialButton and deselect it
                    MaterialButton button = timeSlotButtons.get(slot);
                    if (button != null) {
                        setButtonSelected(button, false);
                    }
                }
                currentSelectedSlots.remove(slot);
                selectedSlotsAdapter.notifyDataSetChanged(); // Update RecyclerView
            }
        });
        recyclerViewTimeSlots.setAdapter(selectedSlotsAdapter);

        // Initialize and display current date
        Calendar today = Calendar.getInstance();
        currentSelectedDate = firestoreDateFormat.format(today.getTime());
        selectedDateTextView.setText("Selected Date: " + displayDateFormat.format(today.getTime()));

        // Set up CalendarView listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);
            currentSelectedDate = firestoreDateFormat.format(selectedCalendar.getTime());
            selectedDateTextView.setText("Selected Date: " + displayDateFormat.format(selectedCalendar.getTime()));
            populateTimeSlots(currentSelectedDate); // Load slots for the newly selected date
        });

        // Set up CheckBox listeners for appointment type
        checkboxInClinic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkboxVideoConsultation.setChecked(false); // Uncheck other type
                currentAppointmentType = "In-clinic";
                populateTimeSlots(currentSelectedDate); // Reload slots for the new type
            } else if (!checkboxVideoConsultation.isChecked()) {
                // Prevent both from being unchecked
                checkboxInClinic.setChecked(true);
                Toast.makeText(AdminManageDoctorAvailabilityActivity.this, "Please select at least one appointment type.", Toast.LENGTH_SHORT).show();
            }
        });

        checkboxVideoConsultation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkboxInClinic.setChecked(false); // Uncheck other type
                currentAppointmentType = "Video Consultation";
                populateTimeSlots(currentSelectedDate); // Reload slots for the new type
            } else if (!checkboxInClinic.isChecked()) {
                // Prevent both from being unchecked
                checkboxVideoConsultation.setChecked(true);
                Toast.makeText(AdminManageDoctorAvailabilityActivity.this, "Please select at least one appointment type.", Toast.LENGTH_SHORT).show();
            }
        });

        // Default to In-clinic checked
        checkboxInClinic.setChecked(true);


        // Set up save button listener
        buttonSaveAvailability.setOnClickListener(v -> saveSelectedSlots());

        // Initially populate slots for today
        populateTimeSlots(currentSelectedDate);
    }

    // Handle back button press in ActionBar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSlotSelection(String slot, MaterialButton button) {
        if (currentlySelectedSlotsSet.contains(slot)) {
            // Deselect the slot
            currentlySelectedSlotsSet.remove(slot);
            setButtonSelected(button, false);
        } else {
            // Select the slot
            currentlySelectedSlotsSet.add(slot);
            setButtonSelected(button, true);
        }
        updateRecyclerViewFromSelectedSet();
    }

    private void setButtonSelected(MaterialButton button, boolean isSelected) {
        if (isSelected) {
            button.setBackgroundColor(getResources().getColor(R.color.colorPrimary, null)); // Or your desired selected color
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundColor(Color.WHITE); // Or your desired deselected color
            button.setTextColor(getResources().getColor(R.color.colorPrimary, null)); // Text color for deselected
        }
    }

    private void updateRecyclerViewFromSelectedSet() {
        currentSelectedSlots.clear();
        currentSelectedSlots.addAll(currentlySelectedSlotsSet);
        Collections.sort(currentSelectedSlots);
        selectedSlotsAdapter.notifyDataSetChanged();
    }


    private void populateTimeSlots(String date) {
        // Reset all time slot buttons to deselected state
        for (MaterialButton button : timeSlotButtons.values()) {
            setButtonSelected(button, false);
        }
        currentlySelectedSlotsSet.clear(); // Clear the tracking set
        currentSelectedSlots.clear(); // Clear the RecyclerView list
        selectedSlotsAdapter.notifyDataSetChanged(); // Notify adapter

        DocumentReference docRef = db.collection("users").document(doctorUidToManage)
                .collection("availability").document(date);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> data = documentSnapshot.getData();
                if (data != null && data.containsKey("slots")) {
                    List<Object> rawSlots = (List<Object>) data.get("slots");
                    if (rawSlots != null) {
                        for (Object item : rawSlots) {
                            if (item instanceof Map) {
                                Map<String, String> slotMap = new HashMap<>();
                                try {
                                    // Attempt to cast directly, if it fails, iterate safely
                                    slotMap = (Map<String, String>) item;
                                } catch (ClassCastException e) {
                                    // Fallback to safer iteration if direct cast fails
                                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                                        if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                                            slotMap.put((String) entry.getKey(), (String) entry.getValue());
                                        }
                                    }
                                }

                                if (slotMap.containsKey("time") && slotMap.containsKey("type")) {
                                    if (slotMap.get("type").equals(currentAppointmentType)) {
                                        String slotTime = slotMap.get("time");
                                        currentlySelectedSlotsSet.add(slotTime);
                                        // Visually select the button
                                        MaterialButton button = timeSlotButtons.get(slotTime);
                                        if (button != null) {
                                            setButtonSelected(button, true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            updateRecyclerViewFromSelectedSet(); // Update RecyclerView based on loaded slots
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching availability for " + date + ": " + e.getMessage());
            Toast.makeText(this, "Error fetching slots: " + e.getMessage(), Toast.LENGTH_LONG).show();
            updateRecyclerViewFromSelectedSet(); // Still refresh UI even on error
        });
    }


    private void saveSelectedSlots() {
        if (currentSelectedDate == null) {
            Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> sortedSelectedSlots = new ArrayList<>(currentlySelectedSlotsSet);
        Collections.sort(sortedSelectedSlots); // Ensure slots are sorted for consistency

        if (sortedSelectedSlots.isEmpty()) {
            // If no slots are selected for the current type, remove them
            deleteAvailabilityForType(currentSelectedDate, currentAppointmentType);
            return;
        }

        List<Map<String, String>> slotsToSave = new ArrayList<>();
        for (String time : sortedSelectedSlots) {
            Map<String, String> slotMap = new HashMap<>();
            slotMap.put("time", time);
            slotMap.put("type", currentAppointmentType);
            slotMap.put("status", "available"); // Initial status
            slotsToSave.add(slotMap);
        }

        DocumentReference docRef = db.collection("users").document(doctorUidToManage)
                .collection("availability").document(currentSelectedDate);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, String>> finalSlots = new ArrayList<>();
            if (documentSnapshot.exists() && documentSnapshot.contains("slots")) {
                List<Object> rawExistingAllSlots = (List<Object>) documentSnapshot.get("slots");
                if (rawExistingAllSlots != null) {
                    for (Object item : rawExistingAllSlots) {
                        if (item instanceof Map) {
                            Map<String, String> existingSlot = new HashMap<>();
                            try {
                                existingSlot = (Map<String, String>) item;
                            } catch (ClassCastException e) {
                                // Fallback
                                for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                                    if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                                        existingSlot.put((String) entry.getKey(), (String) entry.getValue());
                                    }
                                }
                            }

                            // Keep slots of other types OR occupied slots of current type
                            if (existingSlot.containsKey("type") && existingSlot.containsKey("status")) {
                                if (!existingSlot.get("type").equals(currentAppointmentType) ||
                                        (existingSlot.get("type").equals(currentAppointmentType) && !existingSlot.get("status").equals("available"))) {
                                    finalSlots.add(existingSlot);
                                }
                            }
                        }
                    }
                }
            }
            // Add the newly saved slots for the current type
            finalSlots.addAll(slotsToSave);

            Map<String, Object> data = new HashMap<>();
            data.put("slots", finalSlots);

            docRef.set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AdminManageDoctorAvailabilityActivity.this,
                                "Availability updated for " + displayDateFormat.format(getDateFromString(currentSelectedDate)) + " (" + currentAppointmentType + ")",
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Availability saved for " + doctorUidToManage + " on " + currentSelectedDate);
                        populateTimeSlots(currentSelectedDate); // Refresh UI to show saved state
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AdminManageDoctorAvailabilityActivity.this, "Error saving availability: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error saving availability for " + doctorUidToManage + " on " + currentSelectedDate + ": " + e.getMessage());
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(AdminManageDoctorAvailabilityActivity.this, "Error retrieving existing slots for merge: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error retrieving existing slots for merge: " + e.getMessage());
        });
    }

    private void deleteAvailabilityForType(String date, String type) {
        DocumentReference docRef = db.collection("users").document(doctorUidToManage)
                .collection("availability").document(date);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, String>> slotsToKeep = new ArrayList<>();
            if (documentSnapshot.exists() && documentSnapshot.contains("slots")) {
                List<Object> rawExistingAllSlots = (List<Object>) documentSnapshot.get("slots");
                if (rawExistingAllSlots != null) {
                    for (Object item : rawExistingAllSlots) {
                        if (item instanceof Map) {
                            Map<String, String> existingSlot = new HashMap<>();
                            try {
                                existingSlot = (Map<String, String>) item;
                            } catch (ClassCastException e) {
                                // Fallback
                                for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                                    if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                                        existingSlot.put((String) entry.getKey(), (String) entry.getValue());
                                    }
                                }
                            }

                            // Keep slots of other types OR occupied slots of current type
                            if (existingSlot.containsKey("type") && existingSlot.containsKey("status")) {
                                if (!existingSlot.get("type").equals(type) ||
                                        (existingSlot.get("type").equals(type) && !existingSlot.get("status").equals("available"))) {
                                    slotsToKeep.add(existingSlot);
                                }
                            }
                        }
                    }
                }
            }

            if (slotsToKeep.isEmpty()) {
                docRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AdminManageDoctorAvailabilityActivity.this,
                                    "All availability for " + displayDateFormat.format(getDateFromString(currentSelectedDate)) + " (" + type + ") removed.",
                                    Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Document deleted for " + doctorUidToManage + " on " + currentSelectedDate);
                            populateTimeSlots(currentSelectedDate);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AdminManageDoctorAvailabilityActivity.this, "Error deleting availability: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error deleting document for " + doctorUidToManage + " on " + currentSelectedDate + ": " + e.getMessage());
                        });
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("slots", slotsToKeep);
                docRef.set(data, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AdminManageDoctorAvailabilityActivity.this,
                                    "Availability for " + displayDateFormat.format(getDateFromString(currentSelectedDate)) + " (" + type + ") cleared.",
                                    Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Availability cleared for " + doctorUidToManage + " on " + currentSelectedDate);
                            populateTimeSlots(currentSelectedDate);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AdminManageDoctorAvailabilityActivity.this, "Error clearing availability: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error clearing availability for " + doctorUidToManage + " on " + currentSelectedDate + ": " + e.getMessage());
                        });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(AdminManageDoctorAvailabilityActivity.this, "Error checking document for deletion: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error checking document before clearing type: " + e.getMessage());
        });
    }

    private Date getDateFromString(String dateString) {
        try {
            return firestoreDateFormat.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date string: " + dateString, e);
            return new Date();
        }
    }
}