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
import androidx.appcompat.app.AlertDialog;

// --- MISSING FIREBASE IMPORTS (ADDED/VERIFIED) ---
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
// --- END FIREBASE IMPORTS ---

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// Make PatientAppointmentsActivity implement PaymentResultListener
public class PatientAppointmentsActivity extends AppCompatActivity implements PatientAppointmentAdapter.OnAppointmentActionListener, PaymentResultListener {

    private RecyclerView appointmentsRecyclerView;
    private PatientAppointmentAdapter appointmentAdapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView textViewNoAppointments;

    private ListenerRegistration appointmentsListener;

    private static final String TAG = "PatientAppointments";

    // Store the appointment being paid for temporarily
    private Appointment currentAppointmentToPay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_appointments);

        // Initialize Razorpay Checkout
        Checkout.preload(getApplicationContext());

        appointmentsRecyclerView = findViewById(R.id.recyclerViewPatientAppointments);
        progressBar = findViewById(R.id.progressBar);
        textViewNoAppointments = findViewById(R.id.textViewNoAppointments);

        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appointmentList = new ArrayList<>();
        appointmentAdapter = new PatientAppointmentAdapter(appointmentList, this);
        appointmentsRecyclerView.setAdapter(appointmentAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Appointments");
        }

        fetchPatientAppointments();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchPatientAppointments() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewNoAppointments.setVisibility(View.GONE);
        appointmentsRecyclerView.setVisibility(View.GONE);

        String patientUid = currentUser.getUid();
        Log.d(TAG, "Fetching appointments for patient: " + patientUid);

        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }

        db.collection("appointments")
                .whereEqualTo("patientUid", patientUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        progressBar.setVisibility(View.GONE);

                        if (e != null) {
                            Log.e(TAG, "Error listening for patient appointments: ", e);
                            Toast.makeText(PatientAppointmentsActivity.this, "Error loading appointments: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    @Override
    public void onPayNowClick(Appointment appointment) {
        currentAppointmentToPay = appointment; // Store the current appointment to use in callbacks
        Log.d(TAG, "Pay Now clicked for: " + appointment.getDoctorName() + " - Fee: " + String.format(Locale.getDefault(), "₹%.2f", appointment.getFee()));

        // Check if the payment is already completed or processing
        if (appointment.getPaymentStatus() != null && appointment.getPaymentStatus().equalsIgnoreCase("completed")) {
            Toast.makeText(this, "Payment for this appointment is already completed.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (appointment.getPaymentStatus() != null && appointment.getPaymentStatus().equalsIgnoreCase("processing")) {
            Toast.makeText(this, "Payment for this appointment is already being processed.", Toast.LENGTH_SHORT).show();
            return;
        }

        startRazorpayPayment(appointment);
    }

    private void startRazorpayPayment(Appointment appointment) {
        Checkout checkout = new Checkout();

        try {
            JSONObject options = new JSONObject();
            options.put("name", "MedConnect"); // Your app name
            options.put("description", "Appointment with Dr. " + appointment.getDoctorName());
            //options.put("image", "https://your-app-logo-url.com/logo.png"); // Optional: URL of your app logo
            options.put("currency", "INR"); // Or your desired currency
            options.put("amount", (int)(appointment.getFee() * 100)); // Amount in paisa/cents. Convert rupees to paisa.
            options.put("prefill.email", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "");
            options.put("prefill.contact", "9876543210"); // Optional: Patient's contact number (can be fetched from user profile)
            options.put("theme.color", "#3399cc"); // Optional: Set a custom theme color

            options.put("key", "rzp_test_BLNnsWCBNqL0lt"); // <--- This is the corrected way to set the key

            checkout.open(this, options); // Open the Razorpay payment page

        } catch (Exception e) {
            Log.e(TAG, "Error in starting Razorpay Checkout", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        Log.d(TAG, "Payment successful! Payment ID: " + razorpayPaymentId);
        Toast.makeText(this, "Payment Successful: " + razorpayPaymentId, Toast.LENGTH_SHORT).show();

        if (currentAppointmentToPay != null) {
            updateAppointmentPaymentStatus(currentAppointmentToPay, "completed", razorpayPaymentId);
        } else {
            Log.e(TAG, "onPaymentSuccess called but no currentAppointmentToPay set.");
            Toast.makeText(this, "Payment successful, but failed to update appointment status.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPaymentError(int code, String response) {
        Log.e(TAG, "Payment failed! Code: " + code + ", Response: " + response);
        Toast.makeText(this, "Payment Failed: " + response, Toast.LENGTH_LONG).show();

        if (currentAppointmentToPay != null) {
            updateAppointmentPaymentStatus(currentAppointmentToPay, "failed", null);
        }
    }

    private void updateAppointmentPaymentStatus(Appointment appointment, String status, @Nullable String transactionId) {
        DocumentReference appointmentDocRef = db.collection("appointments").document(appointment.getAppointmentId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentStatus", status);
        if (transactionId != null) {
            updates.put("transactionId", transactionId);
        } else {
            updates.put("transactionId", FieldValue.delete());
        }

        appointmentDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Appointment payment status updated to " + status + " for " + appointment.getAppointmentId());
                    Toast.makeText(PatientAppointmentsActivity.this, "Appointment payment status updated to " + status + ".", Toast.LENGTH_SHORT).show();

                    if (status.equalsIgnoreCase("completed")) {
                        String doctorEmail = appointment.getDoctorEmail();
                        String doctorName = appointment.getDoctorName();
                        String patientName = appointment.getPatientName();
                        String date = appointment.getDate();
                        String time = appointment.getTime();

                        String subject = "Payment Received for Appointment: " + appointment.getAppointmentId();
                        String body = "<p>Dear Dr. " + doctorName + ",</p>" +
                                "<p>Payment has been successfully completed for your appointment with patient " + patientName +
                                " on " + date + " at " + time + ".</p>" +
                                "<p>Transaction ID: " + transactionId + "</p>" +
                                "<p>Thank you for using MedConnect.</p>";
                        EmailSender.sendEmail(doctorEmail, subject, body);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating appointment payment status: " + e.getMessage(), e);
                    Toast.makeText(PatientAppointmentsActivity.this, "Failed to update appointment payment status.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
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
    public void onCancelAppointmentClick(Appointment appointment) {
        Log.d(TAG, "Cancel clicked for appointment: " + appointment.getAppointmentId());

        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment with Dr. " + appointment.getDoctorName() + " on " + appointment.getDate() + " at " + appointment.getTime() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    db.runTransaction((Transaction.Function<Void>) transaction -> {
                        // --- STEP 1: Perform ALL READS first ---
                        // Get the appointment document
                        DocumentReference appointmentDocRef = db.collection("appointments").document(appointment.getAppointmentId());
                        DocumentSnapshot appointmentSnapshot = transaction.get(appointmentDocRef);

                        // Get the doctor's availability document for the specific date
                        // These details must be read from the appointmentSnapshot or passed into the method.
                        // Assuming appointment object already has these fields for safety,
                        // otherwise you'd read them from appointmentSnapshot:
                        // String doctorUid = appointmentSnapshot.getString("doctorUid");
                        // String date = appointmentSnapshot.getString("date");
                        // String time = appointmentSnapshot.getString("time");
                        // String type = appointmentSnapshot.getString("type");

                        String doctorUid = appointment.getDoctorUid();
                        String date = appointment.getDate();
                        String time = appointment.getTime();
                        String type = appointment.getType();

                        if (doctorUid == null || date == null || time == null || type == null) {
                            throw new FirebaseFirestoreException("Missing critical appointment details (doctorUid, date, time, or type) for cancellation.", FirebaseFirestoreException.Code.ABORTED);
                        }

                        DocumentReference availabilityDocRef = db.collection("users").document(doctorUid)
                                .collection("availability").document(date);
                        DocumentSnapshot availabilityDoc = transaction.get(availabilityDocRef); // Read for availability

                        // --- Pre-Write Checks after ALL Reads ---
                        if (!appointmentSnapshot.exists()) {
                            throw new FirebaseFirestoreException("Appointment not found during cancellation!", FirebaseFirestoreException.Code.ABORTED);
                        }

                        String currentStatus = appointmentSnapshot.getString("status");
                        String currentPaymentStatus = appointmentSnapshot.getString("paymentStatus");

                        if (currentStatus == null || currentStatus.equalsIgnoreCase("cancelled") ||
                                currentStatus.equalsIgnoreCase("rejected") || currentStatus.equalsIgnoreCase("completed")) {
                            throw new FirebaseFirestoreException("Cannot cancel an appointment that is already " + currentStatus + ".", FirebaseFirestoreException.Code.ABORTED);
                        }

                        if (!availabilityDoc.exists() || !availabilityDoc.contains("slots")) {
                            throw new FirebaseFirestoreException("Doctor's availability data is missing or malformed for this date.", FirebaseFirestoreException.Code.ABORTED);
                        }

                        List<Map<String, Object>> slotsListRaw = (List<Map<String, Object>>) availabilityDoc.get("slots");
                        if (slotsListRaw == null) {
                            throw new FirebaseFirestoreException("Slots list is null in availability document.", FirebaseFirestoreException.Code.ABORTED);
                        }

                        List<Map<String, Object>> updatedSlotsForWrite = new ArrayList<>(slotsListRaw.size());
                        boolean slotFoundAndUnbooked = false;

                        for (Map<String, Object> slotMap : slotsListRaw) {
                            Map<String, Object> currentSlotCopy = new HashMap<>(slotMap);

                            if (Objects.equals(currentSlotCopy.get("time"), time) &&
                                    Objects.equals(currentSlotCopy.get("type"), type) &&
                                    Boolean.TRUE.equals(currentSlotCopy.get("isBooked"))) {
                                currentSlotCopy.put("isBooked", false);
                                slotFoundAndUnbooked = true;
                                     Log.d(TAG, "Slot marked as unbooked in transaction: " + time + " " + type);
                            }
                            updatedSlotsForWrite.add(currentSlotCopy);
                        }

                        if (!slotFoundAndUnbooked) {
                            throw new FirebaseFirestoreException("Failed to find or mark the specific slot as unbooked. It might have been already free or booked by someone else.", FirebaseFirestoreException.Code.ABORTED);
                        }

                        // --- STEP 2: Perform ALL WRITES after all reads and checks ---
                        transaction.update(
                                appointmentDocRef,
                                "status", "cancelled",
                                "cancellationReason", "Patient cancelled" // Add a cancellation reason
                        );

                        if (appointment.getType() != null && appointment.getType().equalsIgnoreCase("Video Consultation") &&
                                currentPaymentStatus != null && currentPaymentStatus.equalsIgnoreCase("completed")) {
                            transaction.update(
                                    appointmentDocRef,
                                    "paymentStatus", "refund_pending"
                            );
                            Log.d(TAG, "Refund initiation marked for appointment " + appointment.getAppointmentId());
                        }

                        transaction.update(availabilityDocRef, "slots", updatedSlotsForWrite);

                        return null; // Transaction completed successfully
                    }).addOnSuccessListener(aVoid -> {
                        Toast.makeText(PatientAppointmentsActivity.this, "Appointment cancelled successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Appointment cancellation transaction succeeded.");

                        String patientEmail = Objects.requireNonNull(mAuth.getCurrentUser()).getEmail();
                        String doctorEmail = appointment.getDoctorEmail();

                        String patientEmailSubject = "Appointment Cancelled - MedConnect";
                        String patientEmailBody = "<p>Dear Patient,</p>" +
                                "<p>Your appointment with Dr. " + appointment.getDoctorName() +
                                " on " + appointment.getDate() + " at " + appointment.getTime() +
                                " has been successfully cancelled.</p>";
                        if (appointment.getPaymentStatus() != null && appointment.getPaymentStatus().equalsIgnoreCase("completed")) {
                            patientEmailBody += "<p>A refund has been initiated and will be processed shortly.</p>";
                        }
                        patientEmailBody += "<p>Thank you for using MedConnect.</p>";
                        EmailSender.sendEmail(patientEmail, patientEmailSubject, patientEmailBody);

                        String doctorEmailSubject = "Appointment Cancelled by Patient - MedConnect";
                        String doctorEmailBody = "<p>Dear Dr. " + appointment.getDoctorName() + ",</p>" +
                                "<p>The appointment with patient " + appointment.getPatientName() +
                                " on " + appointment.getDate() + " at " + appointment.getTime() +
                                " has been cancelled by the patient.</p>" +
                                "<p>The slot is now available again in your schedule.</p>" +
                                "<p>Thank you for using MedConnect.</p>";
                        EmailSender.sendEmail(doctorEmail, doctorEmailSubject, doctorEmailBody);

                    }).addOnFailureListener(e -> {
                        String errorMessage = "Failed to cancel appointment. ";
                        if (e instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                            errorMessage += firestoreException.getMessage();
                            // You can add more specific handling based on firestoreException.getCode() if needed
                        } else {
                            errorMessage += e.getMessage();
                        }
                        Toast.makeText(PatientAppointmentsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error cancelling appointment transaction: " + e.getMessage(), e);
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            Log.d(TAG, "Firestore listener removed in onStop.");
        }
    }
}