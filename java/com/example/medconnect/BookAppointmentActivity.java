package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class BookAppointmentActivity extends AppCompatActivity implements TimeSlotAdapter.OnTimeSlotClickListener {

    private static final String TAG = "BookAppointmentActivity";

    private TextView textViewDoctorName, textViewDoctorSpecialization;
    private CalendarView calendarView;
    private RecyclerView recyclerViewTimeSlots;
    private ProgressBar progressBarAvailability;
    private TextView textViewNoSlots;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String doctorUid;
    private String doctorName;
    private String doctorSpecialization;
    private String doctorEmail;
    private String selectedDateString;

    private TimeSlotAdapter timeSlotAdapter;
    private List<Map<String, String>> availableTimeSlots;

    private ListenerRegistration availabilityListener;

    private static final double DUMMY_APPOINTMENT_FEE = 500.0; // Example fee

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        textViewDoctorName = findViewById(R.id.textViewDoctorName);
        textViewDoctorSpecialization = findViewById(R.id.textViewDoctorSpecialization);
        calendarView = findViewById(R.id.calendarView);
        recyclerViewTimeSlots = findViewById(R.id.recyclerViewTimeSlots);
        progressBarAvailability = findViewById(R.id.progressBarAvailability);
        textViewNoSlots = findViewById(R.id.textViewNoSlots);

        doctorUid = getIntent().getStringExtra("doctorUid");
        doctorName = getIntent().getStringExtra("doctorName");
        doctorSpecialization = getIntent().getStringExtra("doctorSpecialization");
        doctorEmail = getIntent().getStringExtra("doctorEmail");

        textViewDoctorName.setText(doctorName != null ? doctorName : "Doctor Profile");
        textViewDoctorSpecialization.setText(doctorSpecialization != null ? doctorSpecialization : "Specialization Not Set");

        recyclerViewTimeSlots.setLayoutManager(new LinearLayoutManager(this));
        availableTimeSlots = new ArrayList<>();
        timeSlotAdapter = new TimeSlotAdapter(availableTimeSlots, this);
        recyclerViewTimeSlots.setAdapter(timeSlotAdapter);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateString = dateFormat.format(today.getTime());
        fetchAvailableSlots(today.getTime());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
            selectedCalendar.set(Calendar.MINUTE, 0);
            selectedCalendar.set(Calendar.SECOND, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);

            selectedDateString = dateFormat.format(selectedCalendar.getTime());
            fetchAvailableSlots(selectedCalendar.getTime());
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Book Appointment");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchAvailableSlots(Date date) {
        if (doctorUid == null) {
            Toast.makeText(this, "Doctor ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarAvailability.setVisibility(View.VISIBLE);
        textViewNoSlots.setVisibility(View.GONE);
        recyclerViewTimeSlots.setVisibility(View.GONE);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = dateFormat.format(date);
        Log.d(TAG, "Fetching slots for Doctor UID: " + doctorUid + " on date: " + dateString);

        if (availabilityListener != null) {
            availabilityListener.remove();
        }

        availabilityListener = db.collection("users").document(doctorUid)
                .collection("availability").document(dateString)
                .addSnapshotListener((documentSnapshot, e) -> {
                    progressBarAvailability.setVisibility(View.GONE);

                    if (e != null) {
                        Log.e(TAG, "Error listening for slots: " + e.getMessage(), e);
                        Toast.makeText(BookAppointmentActivity.this, "Error loading slots: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        availableTimeSlots.clear();
                        timeSlotAdapter.notifyDataSetChanged();
                        textViewNoSlots.setText("Error loading availability.");
                        textViewNoSlots.setVisibility(View.VISIBLE);
                        recyclerViewTimeSlots.setVisibility(View.GONE);
                        return;
                    }

                    List<Map<String, String>> newFetchedSlots = new ArrayList<>();
                    if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("slots")) {
                        // CHANGE STARTS HERE
                        Object slotsData = documentSnapshot.get("slots");
                        Map<String, Map<String, Object>> loadedSlotsMap = null;

                        if (slotsData instanceof Map) {
                            loadedSlotsMap = (Map<String, Map<String, Object>>) slotsData;
                        } else if (slotsData instanceof List) {
                            // This case handles if you decide to change your Firestore structure to an array later
                            // or if there are legacy documents with array format
                            List<Map<String, Object>> loadedSlotsList = (List<Map<String, Object>>) slotsData;
                            for (Map<String, Object> slotMap : loadedSlotsList) {
                                String time = (String) slotMap.get("time");
                                String type = (String) slotMap.get("type");
                                Boolean isBooked = (Boolean) slotMap.get("isBooked");
                                if (isBooked == null) {
                                    isBooked = false;
                                }

                                if (time != null && type != null) {
                                    Map<String, String> slotData = new HashMap<>();
                                    slotData.put("time", time);
                                    slotData.put("type", type);
                                    slotData.put("isBooked", String.valueOf(isBooked));
                                    newFetchedSlots.add(slotData);
                                }
                            }
                            // Skip the rest of the map processing if it's already processed as a list
                            loadedSlotsMap = null; // Set to null to avoid processing again
                        }

                        if (loadedSlotsMap != null) {
                            // Iterate through the values of the map
                            for (Map<String, Object> slotMap : loadedSlotsMap.values()) { // Corrected line
                                String time = (String) slotMap.get("time");
                                String type = (String) slotMap.get("type");
                                Boolean isBooked = (Boolean) slotMap.get("isBooked");
                                if (isBooked == null) {
                                    isBooked = false;
                                }

                                if (time != null && type != null) {
                                    Map<String, String> slotData = new HashMap<>();
                                    slotData.put("time", time);
                                    slotData.put("type", type);
                                    slotData.put("isBooked", String.valueOf(isBooked));
                                    newFetchedSlots.add(slotData);
                                }
                            }
                            Collections.sort(newFetchedSlots, (s1, s2) -> {
                                String time1 = s1.get("time");
                                String time2 = s2.get("time");
                                return Objects.requireNonNull(time1).compareTo(Objects.requireNonNull(time2));
                            });
                            Log.d(TAG, "Slots found (including booked): " + newFetchedSlots.size());
                        } else if (!(slotsData instanceof List)) { // Log if it's not a list or map, indicating unexpected type
                            Log.d(TAG, "'slots' field is not a recognized type (Map or List) for " + dateString);
                        }
                        // CHANGE ENDS HERE
                    } else {
                        Log.d(TAG, "Document for date " + dateString + " does not exist or missing 'slots' field.");
                    }

                    availableTimeSlots.clear();
                    availableTimeSlots.addAll(newFetchedSlots);
                    timeSlotAdapter.notifyDataSetChanged();

                    if (!availableTimeSlots.isEmpty()) {
                        recyclerViewTimeSlots.setVisibility(View.VISIBLE);
                        textViewNoSlots.setVisibility(View.GONE);
                    } else {
                        textViewNoSlots.setText("No available slots for this date.");
                        textViewNoSlots.setVisibility(View.VISIBLE);
                        recyclerViewTimeSlots.setVisibility(View.GONE);
                    }
                });
    }
    @Override
    public void onTimeSlotClick(Map<String, String> timeSlotMap) {
        String timeRange = timeSlotMap.get("time");
        String type = timeSlotMap.get("type");
        boolean isBooked = Boolean.parseBoolean(Objects.requireNonNull(timeSlotMap.get("isBooked")));

        if (isBooked) {
            Toast.makeText(this, "This slot is already booked.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (doctorUid == null || doctorName == null || doctorSpecialization == null || doctorEmail == null) {
            Toast.makeText(this, "Doctor details are incomplete. Cannot book appointment.", Toast.LENGTH_SHORT).show();
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Confirm Appointment")
                .setMessage("Book appointment with " + doctorName +
                        " on " + selectedDateString + " at " + timeRange + "?\n" +
                        "Type: " + type + "\nFee: ₹" + String.format("%.2f", DUMMY_APPOINTMENT_FEE))
                .setPositiveButton("Yes", (dialog, which) -> {
                    String currentUserUid = null;
                    String currentUserEmail = null;
                    // Get patient's actual name for email/record (if available in User profile)
                    // String currentUserName = "Patient Name"; // TODO: Fetch from Firebase User profile
                    if (auth.getCurrentUser() != null) {
                        currentUserUid = auth.getCurrentUser().getUid();
                        currentUserEmail = auth.getCurrentUser().getEmail();
                    }

                    if (currentUserUid == null || currentUserEmail == null) {
                        Toast.makeText(BookAppointmentActivity.this, "You must be logged in to book an appointment.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // For patient's name, fetch from 'users' collection if available
                    String finalCurrentUserUid = currentUserUid;
                    String finalCurrentUserEmail = currentUserEmail;
                    db.collection("users").document(currentUserUid).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String patientName = documentSnapshot.getString("username"); // or "name"
                                if (patientName == null || patientName.isEmpty()) {
                                    patientName = finalCurrentUserEmail; // Fallback to email
                                }
                                bookAppointment(doctorUid, doctorName, doctorSpecialization, doctorEmail,
                                        finalCurrentUserUid, patientName, finalCurrentUserEmail, selectedDateString, timeRange, type, DUMMY_APPOINTMENT_FEE);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching patient name: " + e.getMessage());
                                Toast.makeText(BookAppointmentActivity.this, "Error getting patient profile. Booking without name.", Toast.LENGTH_SHORT).show();
                                bookAppointment(doctorUid, doctorName, doctorSpecialization, doctorEmail,
                                        finalCurrentUserUid, finalCurrentUserEmail, finalCurrentUserEmail, selectedDateString, timeRange, type, DUMMY_APPOINTMENT_FEE); // Pass email as name
                            });

                })
                .setNegativeButton("No", null)
                .show();
    }

    private void bookAppointment(String doctorUid, String doctorName, String doctorSpecialization, String doctorEmail,
                                 String patientUid, String patientName, String patientEmail, // Added patientName
                                 String date, String time, String type, double apptFee) {

        boolean isSlotActuallyAvailable = false;
        for (Map<String, String> slot : availableTimeSlots) {
            if (Objects.equals(slot.get("time"), time) && Objects.equals(slot.get("type"), type) && !Boolean.parseBoolean(Objects.requireNonNull(slot.get("isBooked")))) {
                isSlotActuallyAvailable = true;
                break;
            }
        }

        if (!isSlotActuallyAvailable) {
            Toast.makeText(this, "The selected slot is no longer available. Please refresh.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference newAppointmentRef = db.collection("appointments").document();

        final String finalMeetingId;
        final String finalMeetingLink;

        if (type.equalsIgnoreCase("Video Consultation")) {
            finalMeetingId = "medconnect-" + newAppointmentRef.getId();
            finalMeetingLink = "https://meet.jit.si/" + finalMeetingId;
        } else {
            finalMeetingId = null;
            finalMeetingLink = null;
        }

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("doctorUid", doctorUid);
        appointmentData.put("doctorName", doctorName);
        appointmentData.put("doctorSpecialization", doctorSpecialization);
        appointmentData.put("doctorEmail", doctorEmail);
        appointmentData.put("patientUid", patientUid);
        appointmentData.put("patientName", patientName); // Add patientName to appointment data
        appointmentData.put("patientEmail", patientEmail);
        appointmentData.put("date", date);
        appointmentData.put("time", time);
        appointmentData.put("type", type);
        appointmentData.put("status", "pending"); // Initial status
        appointmentData.put("paymentStatus", "pending"); // CRUCIAL: Set initial payment status
        appointmentData.put("fee", apptFee);             // CRUCIAL: Set the fee
        appointmentData.put("timestamp", FieldValue.serverTimestamp());
        //appointmentData.put("isMeetingLinkAccessed", false); // <-- ADD THIS LINE
// <--- CHANGED THIS LINE
        // Using System.currentTimeMillis() for long

        if (finalMeetingLink != null) {
            appointmentData.put("meetingId", finalMeetingId);
            appointmentData.put("meetingLink", finalMeetingLink);
        }

        db.runTransaction(transaction -> {
            DocumentReference availabilityDocRef = db.collection("users").document(doctorUid)
                    .collection("availability").document(date);

            DocumentSnapshot availabilityDoc = transaction.get(availabilityDocRef);

            if (!availabilityDoc.exists() || !availabilityDoc.contains("slots")) {
                throw new RuntimeException("Doctor's availability data is missing or malformed for this date.");
            }

            List<Map<String, Object>> slotsListRaw = (List<Map<String, Object>>) availabilityDoc.get("slots");
            if (slotsListRaw == null) {
                throw new RuntimeException("Slots list is null in availability document.");
            }

            List<Map<String, Object>> updatedSlotsForWrite = new ArrayList<>(slotsListRaw.size());
            boolean slotMarkedAsBooked = false;

            for (Map<String, Object> slotMap : slotsListRaw) {
                Map<String, Object> currentSlotCopy = new HashMap<>(slotMap);

                if (Objects.equals(currentSlotCopy.get("time"), time) && Objects.equals(currentSlotCopy.get("type"), type)) {
                    Boolean currentIsBooked = (Boolean) currentSlotCopy.get("isBooked");
                    if (Boolean.FALSE.equals(currentIsBooked)) {
                        currentSlotCopy.put("isBooked", true);
                        slotMarkedAsBooked = true;
                        Log.d(TAG, "Slot marked as booked in transaction: " + time + " " + type);
                    } else {
                        throw new RuntimeException("Selected slot was already booked.");
                    }
                }
                updatedSlotsForWrite.add(currentSlotCopy);
            }

            if (!slotMarkedAsBooked) {
                throw new RuntimeException("Failed to find or mark the specific slot as booked (might have been booked by someone else).");
            }

            transaction.update(availabilityDocRef, "slots", updatedSlotsForWrite);
            transaction.set(newAppointmentRef, appointmentData);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Appointment booking and slot update completed successfully in transaction!");
            Toast.makeText(BookAppointmentActivity.this, "Appointment booked successfully!", Toast.LENGTH_LONG).show();

            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<p>Dear Patient,</p>")
                    .append("<p>Your appointment has been successfully booked!</p>")
                    .append("<p><strong>Appointment Details:</strong></p>")
                    .append("<ul>")
                    .append("<li>Doctor: ").append(doctorName).append("</li>")
                    .append("<li>Date: ").append(date).append("</li>")
                    .append("<li>Time: ").append(time).append("</li>")
                    .append("<li>Type: ").append(type).append("</li>")
                    .append("<li>Fee: ₹").append(String.format("%.2f", apptFee)).append("</li>")
                    .append("<li>Payment Status: Pending</li>")
                    .append("<li>Status: Pending Doctor's Approval</li>")
                    .append("</ul>");

            if (finalMeetingLink != null) {
                emailBody.append("<p>For your video consultation, please use the following link when the appointment is accepted and payment is done:</p>")
                        .append("<p><a href=\"").append(finalMeetingLink).append("\">").append(finalMeetingLink).append("</a></p>")
                        .append("<p>Meeting ID: <strong>").append(finalMeetingId).append("</strong></p>");
            }

            emailBody.append("<p>You will receive another notification once the doctor accepts or rejects your appointment.</p>")
                    .append("<p>Thank you for using MedConnect.</p>");

            EmailSender.sendEmail(patientEmail, "Appointment Confirmation - MedConnect", emailBody.toString());

            Intent intent = new Intent(BookAppointmentActivity.this, PatientAppointmentsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failed: " + e.getMessage(), e);
            Toast.makeText(BookAppointmentActivity.this, "Failed to book appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
            fetchAvailableSlots(new Date());
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (availabilityListener != null) {
            availabilityListener.remove();
        }
    }
}