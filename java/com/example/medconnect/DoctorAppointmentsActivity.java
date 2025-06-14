package com.example.medconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog; // For AlertDialog.Builder

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction; // For Firestore transactions

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DoctorAppointmentsActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentActionListener {

    private RecyclerView appointmentsRecyclerView;
    private AppointmentAdapter appointmentAdapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView textViewNoAppointments;

    private ListenerRegistration appointmentsListener;

    private static final String TAG = "DoctorAppointments";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointments); // Ensure this is your doctor appointments layout

        appointmentsRecyclerView = findViewById(R.id.recyclerViewDoctorAppointments); // Verify this ID in your XML
        progressBar = findViewById(R.id.progressBarDoctor); // Verify this ID in your XML (changed to differentiate)
        textViewNoAppointments = findViewById(R.id.textViewNoDoctorAppointments); // Verify this ID in your XML

        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appointmentList = new ArrayList<>();
        // 'this' refers to the activity implementing the listener interface
        appointmentAdapter = new AppointmentAdapter(appointmentList, this);
        appointmentsRecyclerView.setAdapter(appointmentAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup ActionBar for back button and title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Appointments");
        }

        fetchDoctorAppointments(); // Initial data fetch
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchDoctorAppointments() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Doctor not logged in.", Toast.LENGTH_SHORT).show();
            // Redirect to login or handle unauthenticated state
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewNoAppointments.setVisibility(View.GONE);
        appointmentsRecyclerView.setVisibility(View.GONE);

        String doctorUid = currentUser.getUid();
        Log.d(TAG, "Fetching appointments for doctor: " + doctorUid);

        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }

        appointmentsListener = db.collection("appointments")
                .whereEqualTo("doctorUid", doctorUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        progressBar.setVisibility(View.GONE);

                        if (e != null) {
                            Log.e(TAG, "Error listening for doctor appointments: ", e);
                            Toast.makeText(DoctorAppointmentsActivity.this, "Error loading appointments: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            textViewNoAppointments.setText("Failed to load appointments.");
                            textViewNoAppointments.setVisibility(View.VISIBLE);
                            appointmentsRecyclerView.setVisibility(View.GONE);
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            appointmentList.clear();
                            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                try {
                                    Appointment appointment = document.toObject(Appointment.class);
                                    if (appointment != null) {
                                        appointment.setAppointmentId(document.getId());
                                        appointmentList.add(appointment);
                                    }
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error converting document to Appointment: " + ex.getMessage(), ex);
                                    // Continue to process other documents
                                }
                            }
                            appointmentAdapter.notifyDataSetChanged();

                            if (appointmentList.isEmpty()) {
                                textViewNoAppointments.setText("No appointments found.");
                                textViewNoAppointments.setVisibility(View.VISIBLE);
                                appointmentsRecyclerView.setVisibility(View.GONE);
                            } else {
                                textViewNoAppointments.setVisibility(View.GONE);
                                appointmentsRecyclerView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Log.d(TAG, "Current data: null (no snapshots)");
                            textViewNoAppointments.setText("No appointments found.");
                            textViewNoAppointments.setVisibility(View.VISIBLE);
                            appointmentsRecyclerView.setVisibility(View.GONE);
                        }
                    }
                });
    }

    // --- Implementation of AppointmentAdapter.OnAppointmentActionListener ---

    @Override
    public void onAcceptAppointmentClick(Appointment appointment) {
        Log.d(TAG, "Accept clicked for appointment: " + appointment.getAppointmentId());

        new AlertDialog.Builder(this)
                .setTitle("Accept Appointment")
                .setMessage("Are you sure you want to accept this appointment with " + appointment.getPatientName() + "?")
                .setPositiveButton("Yes, Accept", (dialog, which) -> {
                    // Start a Firestore transaction for atomicity
                    db.runTransaction(transaction -> {
                        DocumentReference appointmentDocRef = db.collection("appointments").document(appointment.getAppointmentId());
                        DocumentSnapshot appointmentSnapshot = transaction.get(appointmentDocRef);

                        if (!appointmentSnapshot.exists()) {
                            throw new RuntimeException("Appointment not found during acceptance!");
                        }

                        String currentStatus = appointmentSnapshot.getString("status");
                        if (currentStatus == null || !currentStatus.equalsIgnoreCase("pending")) {
                            throw new RuntimeException("Cannot accept an appointment that is not pending. Current status: " + currentStatus);
                        }

                        // 1. Update appointment status to "accepted"
                        transaction.update(appointmentDocRef, "status", "accepted");

                        // 2. Optionally set a meeting link if it's a video consultation and not already set
                        if (appointment.getType() != null && appointment.getType().equalsIgnoreCase("Video Consultation") &&
                                (appointment.getMeetingLink() == null || appointment.getMeetingLink().isEmpty())) {
                            // Generate a simple Jitsi Meet link. For production, consider secure generation.
                            String meetingRoomName = appointment.getAppointmentId(); // Unique room name
                            String jitsiLink = "https://meet.jit.si/" + meetingRoomName;
                            transaction.update(appointmentDocRef, "meetingLink", jitsiLink, "meetingId", meetingRoomName);
                            Log.d(TAG, "Generated Jitsi link: " + jitsiLink);
                        }

                        return null; // Transaction success
                    }).addOnSuccessListener(aVoid -> {
                        Toast.makeText(DoctorAppointmentsActivity.this, "Appointment accepted!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Appointment acceptance transaction succeeded.");

                        // Send acceptance email to patient
                        String patientEmail = appointment.getPatientEmail();
                        if (patientEmail != null && !patientEmail.isEmpty()) {
                            String subject = "Your Appointment with Dr. " + appointment.getDoctorName() + " is Confirmed!";
                            String body = "<p>Dear " + appointment.getPatientName() + ",</p>" +
                                    "<p>Your appointment with Dr. " + appointment.getDoctorName() +
                                    " on " + appointment.getDate() + " at " + appointment.getTime() +
                                    " has been accepted.</p>" +
                                    "<p>Appointment Type: " + appointment.getType() + "</p>";

                            if (appointment.getType() != null && appointment.getType().equalsIgnoreCase("Video Consultation")) {
                                // Assuming meetingLink is now updated in Firestore for the transaction
                                // You might need to fetch the updated appointment to get the new link,
                                // or pass the newly generated link from the transaction result if possible.
                                // For simplicity, we'll assume the link is generated based on ID here.
                                String meetingRoomName = appointment.getAppointmentId();
                                String jitsiLink = "https://meet.jit.si/" + meetingRoomName;
                                body += "<p>Join your video consultation here: <a href=\"" + jitsiLink + "\">" + jitsiLink + "</a></p>";
                            }

                            body += "<p>Thank you for using MedConnect.</p>";
                            EmailSender.sendEmail(patientEmail, subject, body);
                        } else {
                            Log.w(TAG, "Patient email not available for sending acceptance notification.");
                        }

                        // UI will auto-update due to addSnapshotListener
                    }).addOnFailureListener(e -> {
                        Toast.makeText(DoctorAppointmentsActivity.this, "Failed to accept appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error accepting appointment transaction: " + e.getMessage(), e);
                    });
                })
                .setNegativeButton("No", null) // Do nothing if "No" is clicked
                .show();
    }

    // THIS METHOD IS NOW CORRECTLY IMPLEMENTED
    @Override
    public void onRejectAppointmentClick(Appointment appointment) {
        Log.d(TAG, "Reject clicked for appointment: " + appointment.getAppointmentId());

        new AlertDialog.Builder(this)
                .setTitle("Reject Appointment")
                .setMessage("Are you sure you want to reject this appointment with " + appointment.getPatientName() + "? This action cannot be undone.")
                .setPositiveButton("Yes, Reject", (dialog, which) -> {
                    // Start a Firestore transaction for atomicity
                    db.runTransaction(transaction -> {
                        DocumentReference appointmentDocRef = db.collection("appointments").document(appointment.getAppointmentId());
                        DocumentSnapshot appointmentSnapshot = transaction.get(appointmentDocRef);

                        if (!appointmentSnapshot.exists()) {
                            throw new RuntimeException("Appointment not found during rejection!");
                        }

                        String currentStatus = appointmentSnapshot.getString("status");
                        if (currentStatus == null || !currentStatus.equalsIgnoreCase("pending")) {
                            throw new RuntimeException("Cannot reject an appointment that is not pending. Current status: " + currentStatus);
                        }

                        // 1. Update appointment status to "rejected"
                        transaction.update(appointmentDocRef, "status", "rejected");

                        // 2. Mark the slot as unbooked in doctor's availability
                        String doctorUid = appointment.getDoctorUid();
                        String date = appointment.getDate(); // This is the date string like "yyyy-MM-dd"
                        String time = appointment.getTime();
                        String type = appointment.getType();

                        DocumentReference availabilityDocRef = db.collection("users").document(doctorUid)
                                .collection("availability").document(date);

                        DocumentSnapshot availabilityDoc = transaction.get(availabilityDocRef);

                        if (availabilityDoc.exists() && availabilityDoc.contains("slots")) {
                            List<Map<String, Object>> slotsListRaw = (List<Map<String, Object>>) availabilityDoc.get("slots");
                            if (slotsListRaw != null) {
                                List<Map<String, Object>> updatedSlotsForWrite = new ArrayList<>(slotsListRaw.size());
                                boolean slotFoundAndUnbooked = false;

                                for (Map<String, Object> slotMap : slotsListRaw) {
                                    Map<String, Object> currentSlotCopy = new HashMap<>(slotMap);

                                    // Find the specific slot by time AND type, and check if it's currently booked

                                    if (Objects.equals(currentSlotCopy.get("time"), time) &&
                                            Objects.equals(currentSlotCopy.get("type"), type) &&
                                            Boolean.TRUE.equals(currentSlotCopy.get("isBooked"))) { // Make sure it was actually booked
                                        currentSlotCopy.put("isBooked", false); // Mark as unbooked
                                        slotFoundAndUnbooked = true;
                                        Log.d(TAG, "Slot marked as unbooked in transaction for rejection: " + time + " " + type);
                                    }
                                    updatedSlotsForWrite.add(currentSlotCopy); // Add all slots, including the modified one
                                }

                                if (slotFoundAndUnbooked) {
                                    transaction.update(availabilityDocRef, "slots", updatedSlotsForWrite);
                                } else {
                                    Log.w(TAG, "Slot not found or already unbooked in doctor's availability for rejection: " + time + " " + type);
                                }
                            }
                        } else {
                            Log.w(TAG, "Doctor's availability document for " + date + " does not exist or has no slots during rejection attempt.");
                        }

                        return null; // Transaction success
                    }).addOnSuccessListener(aVoid -> {
                        Toast.makeText(DoctorAppointmentsActivity.this, "Appointment rejected!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Appointment rejection transaction succeeded.");

                        // Send rejection email to patient
                        String patientEmail = appointment.getPatientEmail();
                        if (patientEmail != null && !patientEmail.isEmpty()) {
                            String subject = "Your Appointment with Dr. " + appointment.getDoctorName() + " has been Rejected";
                            String body = "<p>Dear " + appointment.getPatientName() + ",</p>" +
                                    "<p>We regret to inform you that your appointment with Dr. " + appointment.getDoctorName() +
                                    " on " + appointment.getDate() + " at " + appointment.getTime() +
                                    " has been rejected.</p>" +
                                    "<p>You may try booking another appointment or contact the doctor directly.</p>" +
                                    "<p>Thank you for using MedConnect.</p>";
                            EmailSender.sendEmail(patientEmail, subject, body);
                        } else {
                            Log.w(TAG, "Patient email not available for sending rejection notification.");
                        }
                        // UI will auto-update due to addSnapshotListener
                    }).addOnFailureListener(e -> {
                        Toast.makeText(DoctorAppointmentsActivity.this, "Failed to reject appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error rejecting appointment transaction: " + e.getMessage(), e);
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override // THIS OVERRIDE SHOULD NOW BE CORRECT
    public void onJoinMeetingClick(Appointment appointment) {
        String meetingLink = appointment.getMeetingLink();
        if (meetingLink != null && !meetingLink.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(meetingLink));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No application can handle this meeting link (e.g., browser or Jitsi Meet app).", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "No app to handle URL: " + meetingLink);
            }
        } else {
            Toast.makeText(this, "Meeting link not available yet for this appointment.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            Log.d(TAG, "Firestore listener removed in onStop (DoctorAppointmentsActivity).");
        }
    }
}