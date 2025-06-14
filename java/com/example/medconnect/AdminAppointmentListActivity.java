package com.example.medconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration; // Import for Firestore listener

import java.util.ArrayList;
import java.util.List;

public class AdminAppointmentListActivity extends AppCompatActivity {

    private RecyclerView appointmentRecyclerView;
    private AppointmentAdapter appointmentAdapter; // You already have this
    private List<Appointment> appointmentList; // You already have this model
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView textViewNoAppointments;

    private ListenerRegistration appointmentsListener; // To manage the Firestore listener

    private static final String TAG = "AdminAppointments";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_appointment_list); // Ensure this XML is created

        // Setup ActionBar for back button and title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Appointments");
        }

        appointmentRecyclerView = findViewById(R.id.recyclerViewAppointments);
        progressBar = findViewById(R.id.progressBarAppointments);
        textViewNoAppointments = findViewById(R.id.textViewNoAppointments);

        appointmentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appointmentList = new ArrayList<>();
        // Reuse your existing AppointmentAdapter.
        // Note: If this admin view needs different actions than the doctor view,
        // you might need a separate adapter or modify this one with different listener types.
        // For now, let's assume it just displays. If you want actions, you'd implement
        // AppointmentAdapter.OnAppointmentActionListener here as well or create AdminAppointmentAdapter.
        appointmentAdapter = new AppointmentAdapter(appointmentList, null); // Pass null for listener if no actions
        appointmentRecyclerView.setAdapter(appointmentAdapter);

        db = FirebaseFirestore.getInstance();

        fetchAllAppointments();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Go back to the previous activity (AdminDashboard)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchAllAppointments() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoAppointments.setVisibility(View.GONE);
        appointmentRecyclerView.setVisibility(View.GONE);

        // Remove any previous listener to avoid memory leaks or duplicate data
        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }

        // Fetch all appointments
        appointmentsListener = db.collection("appointments")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Order by timestamp (most recent first)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    progressBar.setVisibility(View.GONE);

                    if (e != null) {
                        Log.e(TAG, "Error listening for appointments: ", e);
                        Toast.makeText(AdminAppointmentListActivity.this, "Error loading appointments: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        textViewNoAppointments.setText("Failed to load appointment list.");
                        textViewNoAppointments.setVisibility(View.VISIBLE);
                        appointmentRecyclerView.setVisibility(View.GONE);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        appointmentList.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            try {
                                Appointment appointment = document.toObject(Appointment.class);
                                if (appointment != null) {
                                    appointment.setAppointmentId(document.getId()); // Set the ID
                                    appointmentList.add(appointment);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error converting document to Appointment object: " + ex.getMessage(), ex);
                            }
                        }
                        appointmentAdapter.notifyDataSetChanged();

                        if (appointmentList.isEmpty()) {
                            textViewNoAppointments.setText("No appointments found.");
                            textViewNoAppointments.setVisibility(View.VISIBLE);
                            appointmentRecyclerView.setVisibility(View.GONE);
                        } else {
                            textViewNoAppointments.setVisibility(View.GONE);
                            appointmentRecyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.d(TAG, "No appointment data found.");
                        textViewNoAppointments.setText("No appointments found.");
                        textViewNoAppointments.setVisibility(View.VISIBLE);
                        appointmentRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            Log.d(TAG, "Firestore listener removed in onStop (AdminAppointmentListActivity).");
        }
    }
}