package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem; // Added for back button
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Added for back button
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // Added for Objects.requireNonNull

// This activity now solely focuses on listing doctors
public class DoctorListActivity extends AppCompatActivity implements DoctorAdapter.OnDoctorClickListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerViewDoctors;
    private ProgressBar progressBarDoctors;
    private TextView textViewNoDoctors; // Added TextView for "No doctors found"

    private DoctorAdapter doctorAdapter;
    private List<Doctor> doctorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure this layout file name is correct after renaming: activity_doctor_list.xml
        setContentView(R.layout.activity_doctor_list);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerViewDoctors = findViewById(R.id.recyclerViewDoctors);
        progressBarDoctors = findViewById(R.id.progressBarDoctors);
        textViewNoDoctors = findViewById(R.id.textViewNoDoctors); // Initialize the new TextView

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(DoctorListActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Set up ActionBar for back button and title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Available Doctors");
        }

        recyclerViewDoctors.setLayoutManager(new LinearLayoutManager(this));
        doctorList = new ArrayList<>();
        doctorAdapter = new DoctorAdapter(doctorList, this);
        recyclerViewDoctors.setAdapter(doctorAdapter);

        loadAllDoctors();
    }

    // Handle back button click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAllDoctors() {
        progressBarDoctors.setVisibility(View.VISIBLE);
        textViewNoDoctors.setVisibility(View.GONE); // Hide initially
        recyclerViewDoctors.setVisibility(View.GONE); // Hide initially

        db.collection("users")
                .whereEqualTo("role", "doctor")
                // Keep this line if you want to only show APPROVED doctors (recommended for patient view):
                //.whereEqualTo("status", "approved")
                .get()
                .addOnCompleteListener(task -> {
                    progressBarDoctors.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        List<Doctor> allDoctors = new ArrayList<>();
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            Doctor doctor = document.toObject(Doctor.class);
                            doctor.setUid(document.getId());
                            allDoctors.add(doctor);
                        }
                        doctorAdapter.setDoctors(allDoctors);
                        if (allDoctors.isEmpty()) {
                            textViewNoDoctors.setVisibility(View.VISIBLE);
                            Toast.makeText(DoctorListActivity.this, "No doctors found.", Toast.LENGTH_SHORT).show();
                        } else {
                            recyclerViewDoctors.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(DoctorListActivity.this, "Error loading doctors: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        textViewNoDoctors.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onDoctorClick(Doctor doctor) {
        String doctorDisplayName;
        if (doctor.getUsername() != null && !doctor.getUsername().isEmpty()) {
            doctorDisplayName = "Dr. " + doctor.getUsername();
        } else {
            doctorDisplayName = "Dr. " + doctor.getEmail();
        }

        Toast.makeText(this, "Booking appointment with: " + doctorDisplayName, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(DoctorListActivity.this, BookAppointmentActivity.class);
        intent.putExtra("doctorUid", doctor.getUid());
        intent.putExtra("doctorName", doctorDisplayName);
        intent.putExtra("doctorSpecialization", doctor.getSpecialization());
        intent.putExtra("doctorEmail", doctor.getEmail());
        startActivity(intent);
    }
}