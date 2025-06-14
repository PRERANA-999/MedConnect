package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Added for logging
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Import CardView

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DoctorDashboardActivity extends AppCompatActivity {

    private static final String TAG = "DoctorDashboardActivity"; // Define a TAG for logging

    private TextView welcomeDoctorTextView;
    // Change Button declarations to CardView for the dashboard items
    private CardView cardMyProfile;
    private CardView cardManageAvailability;
    private CardView cardViewAppointments;
    private CardView cardDoctorSettings; // Added for the optional 4th card

    private Button doctorLogoutButton; // This remains a standard Button

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure this layout file name is correct: activity_doctor_dashboard.xml
        setContentView(R.layout.activity_doctor_dashboard_activity); // Updated layout file name
        Log.d(TAG, "DoctorDashboardActivity onCreate called.");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        welcomeDoctorTextView = findViewById(R.id.welcomeDoctorTextView);

        // Initialize CardViews using their new IDs from the XML
        cardMyProfile = findViewById(R.id.cardMyProfile);
        cardManageAvailability = findViewById(R.id.cardManageAvailability);
        cardViewAppointments = findViewById(R.id.cardViewAppointments);
        cardDoctorSettings = findViewById(R.id.cardDoctorSettings); // Initialize the optional 4th card

        // Initialize the Logout Button
        doctorLogoutButton = findViewById(R.id.doctorLogoutButton);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No current user found. Redirecting to LoginActivity.");
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Display doctor's email in the welcome text
        welcomeDoctorTextView.setText("Welcome, Doctor " + currentUser.getEmail() + "!");

        // Set click listener for My Profile Card
        cardMyProfile.setOnClickListener(v -> {
            Log.d(TAG, "Card 'My Profile' clicked.");
            Intent intent = new Intent(DoctorDashboardActivity.this, DoctorProfileActivity.class);
            startActivity(intent);
        });

        // Set click listener for Manage Availability Card
        cardManageAvailability.setOnClickListener(v -> {
            Log.d(TAG, "Card 'Manage Availability' clicked.");
            Intent intent = new Intent(DoctorDashboardActivity.this, ManageAvailabilityActivity.class);
            startActivity(intent);
        });

        // Set click listener for View Appointments Card
        cardViewAppointments.setOnClickListener(v -> {
            Log.d(TAG, "Card 'View Appointments' clicked.");
            Intent intent = new Intent(DoctorDashboardActivity.this, DoctorAppointmentsActivity.class);
            startActivity(intent);
        });

        // Set click listener for Settings Card (Optional)
        cardDoctorSettings.setOnClickListener(v -> {
            Log.d(TAG, "Card 'Settings' clicked.");
            Toast.makeText(DoctorDashboardActivity.this, "Settings clicked!", Toast.LENGTH_SHORT).show();
            // Implement your settings activity or dialog here
        });


        // Set click listener for Logout button
        doctorLogoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout Button clicked.");
            mAuth.signOut();
            Toast.makeText(DoctorDashboardActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
            startActivity(intent);
            finish();
        });
    }

    // Optional: Re-check role onStart (good practice for security for all users)
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "DoctorDashboardActivity onStart called. Checking user role.");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User null in onStart. Redirecting to LoginActivity.");
            Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Verify if the current user is still a doctor
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            Log.d(TAG, "User role in onStart: " + role);
                            // If role is null or not 'doctor', redirect to login
                            if (role == null || !role.equalsIgnoreCase("doctor")) {
                                Log.w(TAG, "Access Denied: User role is not doctor. Logging out.");
                                Toast.makeText(DoctorDashboardActivity.this, "Access Denied: Not a doctor.", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                                Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            // If role is doctor, stay on dashboard
                        } else {
                            Log.w(TAG, "User document not found in onStart. Logging out.");
                            Toast.makeText(DoctorDashboardActivity.this, "User data not found. Logging out.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking user role in onStart: " + e.getMessage(), e);
                        Toast.makeText(DoctorDashboardActivity.this, "Error checking user role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
        }
    }
}