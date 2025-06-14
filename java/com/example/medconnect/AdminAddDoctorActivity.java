package com.example.medconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminAddDoctorActivity extends AppCompatActivity {

    private EditText editTextDoctorEmail, editTextDoctorPassword, editTextDoctorLicense;
    private Button buttonRegisterDoctor;
    private ProgressBar progressBarDoctorRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_doctor);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextDoctorEmail = findViewById(R.id.editTextDoctorEmail);
        editTextDoctorPassword = findViewById(R.id.editTextDoctorPassword);
        editTextDoctorLicense = findViewById(R.id.editTextDoctorLicense);
        buttonRegisterDoctor = findViewById(R.id.buttonRegisterDoctor);
        progressBarDoctorRegister = findViewById(R.id.progressBarDoctorRegister);

        buttonRegisterDoctor.setOnClickListener(v -> registerNewDoctor());
    }

    private void registerNewDoctor() {
        String email = editTextDoctorEmail.getText().toString().trim();
        String password = editTextDoctorPassword.getText().toString().trim();
        String licenseNumber = editTextDoctorLicense.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextDoctorEmail.setError("Email is required.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextDoctorEmail.setError("Please enter a valid email address.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextDoctorPassword.setError("Password is required.");
            return;
        }
        if (password.length() < 6) {
            editTextDoctorPassword.setError("Password must be at least 6 characters.");
            return;
        }
        if (TextUtils.isEmpty(licenseNumber)) {
            editTextDoctorLicense.setError("License Number is required.");
            return;
        }

        progressBarDoctorRegister.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser newDoctorUser = task.getResult().getUser();
                        if (newDoctorUser != null) {

                            newDoctorUser.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            Toast.makeText(AdminAddDoctorActivity.this,
                                                    "Verification email sent to " + newDoctorUser.getEmail(),
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(AdminAddDoctorActivity.this,
                                                    "Failed to send verification email to doctor: " + verificationTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });

                            Map<String, Object> doctor = new HashMap<>();
                            doctor.put("email", email);
                            doctor.put("role", "doctor");
                            doctor.put("licenseNumber", licenseNumber);
                            doctor.put("firstLoginDone", false); // Mark for first-time password change
                            doctor.put("status", "approved"); // <--- ADDED THIS LINE: Set status to "approved" by admin

                            db.collection("users").document(newDoctorUser.getUid()).set(doctor)
                                    .addOnSuccessListener(aVoid -> {
                                        progressBarDoctorRegister.setVisibility(View.GONE);
                                        Toast.makeText(AdminAddDoctorActivity.this,
                                                "Doctor " + email + " registered successfully! Advise doctor to check email for verification and then use 'Forgot Password?' to set their own password.",
                                                Toast.LENGTH_LONG).show();
                                        editTextDoctorEmail.setText("");
                                        editTextDoctorPassword.setText("");
                                        editTextDoctorLicense.setText("");
                                    })
                                    .addOnFailureListener(e -> {
                                        progressBarDoctorRegister.setVisibility(View.GONE);
                                        Toast.makeText(AdminAddDoctorActivity.this, "Failed to save doctor data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        newDoctorUser.delete(); // Delete Auth user if Firestore save fails
                                    });

                        }
                    } else {
                        progressBarDoctorRegister.setVisibility(View.GONE);
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(AdminAddDoctorActivity.this, "Registration failed: Email already registered.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdminAddDoctorActivity.this, "Doctor registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}