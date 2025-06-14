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

public class AdminPatientListActivity extends AppCompatActivity {

    private RecyclerView patientRecyclerView;
    private UserAdapter patientAdapter; // You'll need a UserAdapter or similar
    private List<User> patientList; // You'll need a User model class
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView textViewNoPatients;

    private ListenerRegistration patientListener; // To manage the Firestore listener

    private static final String TAG = "AdminPatientList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_patient_list); // Ensure this XML is created

        // Setup ActionBar for back button and title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Patients");
        }

        patientRecyclerView = findViewById(R.id.recyclerViewPatients);
        progressBar = findViewById(R.id.progressBarPatients);
        textViewNoPatients = findViewById(R.id.textViewNoPatients);

        patientRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        patientList = new ArrayList<>();
        // Initialize your adapter. Assume UserAdapter takes a List<User>
        // and potentially a click listener if you want to interact with patient items
        patientAdapter = new UserAdapter(patientList); // Create this adapter
        patientRecyclerView.setAdapter(patientAdapter);

        db = FirebaseFirestore.getInstance();

        fetchPatients();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Go back to the previous activity (AdminDashboard)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchPatients() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoPatients.setVisibility(View.GONE);
        patientRecyclerView.setVisibility(View.GONE);

        // Remove any previous listener to avoid memory leaks or duplicate data
        if (patientListener != null) {
            patientListener.remove();
        }

        // Fetch users where role is 'patient'
        patientListener = db.collection("users")
                .whereEqualTo("role", "patient")
                .orderBy("name", Query.Direction.ASCENDING) // Order by name or a relevant field
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    progressBar.setVisibility(View.GONE);

                    if (e != null) {
                        Log.e(TAG, "Error listening for patients: ", e);
                        Toast.makeText(AdminPatientListActivity.this, "Error loading patients: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        textViewNoPatients.setText("Failed to load patient list.");
                        textViewNoPatients.setVisibility(View.VISIBLE);
                        patientRecyclerView.setVisibility(View.GONE);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        patientList.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            try {
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    user.setUid(document.getId()); // Set the UID from document ID
                                    patientList.add(user);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error converting document to User object for patient: " + ex.getMessage(), ex);
                            }
                        }
                        patientAdapter.notifyDataSetChanged();

                        if (patientList.isEmpty()) {
                            textViewNoPatients.setText("No patients found.");
                            textViewNoPatients.setVisibility(View.VISIBLE);
                            patientRecyclerView.setVisibility(View.GONE);
                        } else {
                            textViewNoPatients.setVisibility(View.GONE);
                            patientRecyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.d(TAG, "No patient data found.");
                        textViewNoPatients.setText("No patients found.");
                        textViewNoPatients.setVisibility(View.VISIBLE);
                        patientRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (patientListener != null) {
            patientListener.remove();
            Log.d(TAG, "Firestore listener removed in onStop (AdminPatientListActivity).");
        }
    }
}