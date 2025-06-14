package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Add Log import for debugging
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Import CardView

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity"; // Define a TAG for logging

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView welcomeTextView; // Changed from adminWelcomeTextView to match XML [cite: 1]

    // Declare CardViews for the dashboard items
    private CardView cardAddDoctor;
    private CardView cardViewDoctors;
    private CardView cardViewPatients;
    // Renamed from cardDoctorApplications to cardViewAppointments to match common naming/your previous code,
    // though the XML still uses cardDoctorApplications - ensure this matches your intent for the actual activity it launches.
    // If it's specifically for doctor applications, keep cardDoctorApplications and change the Intent target.
    private CardView cardDoctorApplications; // Keeping the ID as per your provided XML [cite: 1]

    private Button buttonAdminLogout; // This remains a standard Button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard); // Set the correct layout [cite: 1]
        Log.d(TAG, "AdminDashboardActivity onCreate called.");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize TextView - uses ID 'welcomeTextView' from your XML [cite: 1]
        welcomeTextView = findViewById(R.id.welcomeTextView);

        // Initialize CardViews using their IDs from your XML [cite: 1]
        cardAddDoctor = findViewById(R.id.cardAddDoctor);
        cardViewDoctors = findViewById(R.id.cardViewDoctors);
        cardViewPatients = findViewById(R.id.cardViewPatients);
        cardDoctorApplications = findViewById(R.id.cardDoctorApplications); // Initialize based on XML [cite: 1]

        // Initialize the Logout Button
        buttonAdminLogout = findViewById(R.id.buttonAdminLogout);

        // --- User Authentication Check ---
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No current user found. Redirecting to LoginActivity.");
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Finish this activity so user can't go back to it
            return;
        }

        // Display admin's email in the welcome text
        // Note: The welcomeTextView ID from XML is 'welcomeTextView' [cite: 1]
        welcomeTextView.setText("Welcome, Admin " + currentUser.getEmail() + "!");

        // --- Set Click Listeners for CardViews ---

        cardAddDoctor.setOnClickListener(v -> {
            Log.d(TAG, "Card 'Add Doctor' clicked.");
            Intent intent = new Intent(AdminDashboardActivity.this, AdminAddDoctorActivity.class);
            startActivity(intent);
        });

        cardViewDoctors.setOnClickListener(v -> {
            Log.d(TAG, "Card 'View All Doctors' clicked.");
            Intent intent = new Intent(AdminDashboardActivity.this, AdminDoctorListActivity.class);
            startActivity(intent);
        });

        cardViewPatients.setOnClickListener(v -> {
            Log.d(TAG, "Card 'View All Patients' clicked.");
            Intent intent = new Intent(AdminDashboardActivity.this, AdminPatientListActivity.class);
            startActivity(intent);
        });

        cardDoctorApplications.setOnClickListener(v -> { // Listener for "Doctor Applications" card [cite: 1]
            Log.d(TAG, "Card 'Doctor Applications' clicked.");
            Intent intent = new Intent(AdminDashboardActivity.this, DoctorListActivity.class);
            // Assuming you have a DoctorApplicationListActivity
            startActivity(intent);
        });

        // Logout Button listener
        buttonAdminLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout Button clicked.");
            mAuth.signOut();
            Toast.makeText(AdminDashboardActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
            // Redirect to login activity and clear activity stack
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
            startActivity(intent);
            finish();
        });
    }

    // Optional: Re-check role onStart. This is a good practice for security.
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "AdminDashboardActivity onStart called.");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User null in onStart. Redirecting to LoginActivity.");
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Verify if the current user is still an admin by checking their role in Firestore
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            Log.d(TAG, "User role in onStart: " + role);
                            if (role == null || !role.equalsIgnoreCase("admin")) {
                                Log.w(TAG, "Access Denied: User role is not admin. Logging out.");
                                Toast.makeText(AdminDashboardActivity.this, "Access Denied: Not an admin.", Toast.LENGTH_SHORT).show();
                                mAuth.signOut(); // Log them out for security
                                Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            // If role is admin, no redirection needed, stay on dashboard
                        } else {
                            Log.w(TAG, "User document not found in onStart. Logging out.");
                            Toast.makeText(AdminDashboardActivity.this, "User data not found. Logging out.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking user role in onStart: " + e.getMessage(), e);
                        Toast.makeText(AdminDashboardActivity.this, "Error checking user role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
        }
    }
}