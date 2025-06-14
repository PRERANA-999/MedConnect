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

public class AdminDoctorListActivity extends AppCompatActivity {

    private RecyclerView doctorRecyclerView;
    private UserAdapter doctorAdapter; // Reusing UserAdapter
    private List<User> doctorList;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView textViewNoDoctors;

    private ListenerRegistration doctorListener;

    private static final String TAG = "AdminDoctorList"; // Ensure this TAG is used

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_doctor_list); // Ensure this XML is created

        // Setup ActionBar for back button and title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Doctors");
        }

        doctorRecyclerView = findViewById(R.id.recyclerViewDoctors);
        progressBar = findViewById(R.id.progressBarDoctors);
        textViewNoDoctors = findViewById(R.id.textViewNoDoctors);

        doctorRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        doctorList = new ArrayList<>();
        // Initialize your adapter. Assume UserAdapter takes a List<User>
        // and potentially a click listener if you want to interact with doctor items
        doctorAdapter = new UserAdapter(doctorList); // Create this adapter
        doctorRecyclerView.setAdapter(doctorAdapter);

        db = FirebaseFirestore.getInstance();

        fetchDoctors();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Go back to the previous activity (AdminDashboard)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchDoctors() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoDoctors.setVisibility(View.GONE);
        doctorRecyclerView.setVisibility(View.GONE);

        // Remove any previous listener to avoid memory leaks or duplicate data
        if (doctorListener != null) {
            doctorListener.remove();
        }

        // ADDED LOG: Indicate start of query
        Log.d(TAG, "Starting Firestore query for doctors...");

        // Fetch users where role is 'doctor'
        doctorListener = db.collection("users")
                .whereEqualTo("role", "doctor")
                .orderBy("name", Query.Direction.ASCENDING) // Order by name or a relevant field
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    progressBar.setVisibility(View.GONE);

                    if (e != null) {
                        // ADDED LOG: Log Firestore error
                        Log.e(TAG, "Error listening for doctors: ", e);
                        Toast.makeText(AdminDoctorListActivity.this, "Error loading doctors: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        textViewNoDoctors.setText("Failed to load doctor list.");
                        textViewNoDoctors.setVisibility(View.VISIBLE);
                        doctorRecyclerView.setVisibility(View.GONE);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        // ADDED LOG: Log number of documents returned
                        Log.d(TAG, "Query successful. Number of documents: " + queryDocumentSnapshots.size());
                        if (queryDocumentSnapshots.isEmpty()) {
                            // ADDED LOG: Explicitly log if snapshot is empty
                            Log.d(TAG, "Query returned an empty snapshot for doctors.");
                        }

                        doctorList.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            try {
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    user.setUid(document.getId()); // Set the UID from document ID
                                    doctorList.add(user);
                                    // ADDED LOG: Log each added doctor's data using User's toString()
                                    Log.d(TAG, "Added doctor to list: " + user.toString());
                                } else {
                                    // ADDED LOG: Log if toObject returns null
                                    Log.e(TAG, "Document.toObject(User.class) returned null for doctor: " + document.getId() + ". Document data: " + document.getData());
                                }
                            } catch (Exception ex) {
                                // ADDED LOG: Log error during conversion, include document ID and data
                                Log.e(TAG, "Error converting document to User object for doctor. Document ID: " + document.getId() + ". Data: " + document.getData(), ex);
                            }
                        }
                        doctorAdapter.notifyDataSetChanged();
                        // ADDED LOG: Log final list size
                        Log.d(TAG, "Adapter notified. Final doctorList size: " + doctorList.size());


                        if (doctorList.isEmpty()) {
                            textViewNoDoctors.setText("No doctors found.");
                            textViewNoDoctors.setVisibility(View.VISIBLE);
                            doctorRecyclerView.setVisibility(View.GONE);
                        } else {
                            textViewNoDoctors.setVisibility(View.GONE);
                            doctorRecyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // ADDED LOG: Log if queryDocumentSnapshots is null (unlikely but for completeness)
                        Log.d(TAG, "queryDocumentSnapshots was null for doctors.");
                        textViewNoDoctors.setText("No doctors found.");
                        textViewNoDoctors.setVisibility(View.VISIBLE);
                        doctorRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (doctorListener != null) {
            doctorListener.remove();
            Log.d(TAG, "Firestore listener removed in onStop (AdminDoctorListActivity).");
        }
    }
}