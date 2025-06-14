package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View; // Keep this import as it's used for View.GONE
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Keep for Firestore tasks if needed
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // Import for Firestore
import com.google.firebase.firestore.FirebaseFirestore; // Import for Firestore
// Remove Realtime Database imports:
// import com.google.firebase.database.DataSnapshot;
// import com.google.firebase.database.DatabaseError;
// import com.google.firebase.database.DatabaseReference;
// import com.google.firebase.database.FirebaseDatabase;
// import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeTextView;
    private Button logoutButton;
    // private Button applyDoctorButton; // REMOVE THIS DECLARATION
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Using Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        welcomeTextView = findViewById(R.id.welcomeTextView);
        logoutButton = findViewById(R.id.logoutButton);
        // applyDoctorButton = findViewById(R.id.applyDoctorButton); // REMOVE THIS INITIALIZATION

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, redirect to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Fetch user role from Firestore
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists() && document.contains("role")) {
                            String role = document.getString("role");
                            welcomeTextView.setText("Welcome, " + currentUser.getEmail() + " (" + role + ")!");
                            // Removed the applyDoctorButton visibility logic as it's gone
                        } else {
                            welcomeTextView.setText("Welcome, " + currentUser.getEmail() + " (Role Unknown)!");
                            Toast.makeText(MainActivity.this, "User data not found or role missing. Please contact support.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        welcomeTextView.setText("Welcome, " + currentUser.getEmail() + " (Error fetching role)!");
                        Toast.makeText(MainActivity.this, "Failed to fetch role: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // REMOVE THIS BLOCK (applyDoctorButton.setOnClickListener)
        /*
        applyDoctorButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DoctorApplicationActivity.class);
            startActivity(intent);
        });
        */
    }

    // Keep onStart simple for MainActivity as it's a destination activity after login
    @Override
    protected void onStart() {
        super.onStart();
        // The logic for handling user login and redirection to MainActivity/DoctorDashboard/AdminDashboard
        // is now handled completely within LoginActivity.
        // MainActivity itself just loads its content assuming a user is already logged in and verified.
    }
}