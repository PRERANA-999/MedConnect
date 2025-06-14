package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Hide the ActionBar for a full-screen welcome experience
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();

        Button buttonLogin = findViewById(R.id.buttonGetStarted); // Using buttonGetStarted for Login
        Button buttonRegister = findViewById(R.id.buttonRegister);

        buttonLogin.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });

        buttonRegister.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If a user is already logged in, check their role and redirect
            // This logic might be better placed after successful role check in LoginActivity
            // but for a seamless re-launch, you might consider basic redirection here.
            // For simplicity now, we just let them go to login.
            // In a real app, you'd check role and go directly to patient/doctor/admin dashboard
            // if they are already logged in and their session is valid.
            // For example:
            // Intent intent;
            // if (user.getRole().equals("patient")) {
            //    intent = new Intent(WelcomeActivity.this, PatientDashboardActivity.class);
            // } else if (user.getRole().equals("doctor")) {
            //    intent = new Intent(WelcomeActivity.this, DoctorDashboardActivity.class);
            // } else if (user.getRole().equals("admin")) {
            //    intent = new Intent(WelcomeActivity.this, AdminDashboardActivity.class);
            // } else {
            //    intent = new Intent(WelcomeActivity.this, LoginActivity.class); // Default to login if role unclear
            // }
            // startActivity(intent);
            // finish(); // Close WelcomeActivity
        }
    }
}