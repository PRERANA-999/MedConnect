package com.example.medconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DoctorApplicationActivity extends AppCompatActivity {

    private EditText editTextLicenseNumber;
    private Button buttonSubmitApplication;
    private ProgressBar progressBarApplication;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_application);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextLicenseNumber = findViewById(R.id.editTextLicenseNumber);
        buttonSubmitApplication = findViewById(R.id.buttonSubmitApplication);
        progressBarApplication = findViewById(R.id.progressBarApplication);

        buttonSubmitApplication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitApplication();
            }
        });
    }

    private void submitApplication() {
        String licenseNumber = editTextLicenseNumber.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (TextUtils.isEmpty(licenseNumber)) {
            editTextLicenseNumber.setError("Medical License Number is required.");
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to apply.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarApplication.setVisibility(View.VISIBLE);

        Map<String, Object> application = new HashMap<>();
        application.put("userId", currentUser.getUid());
        application.put("email", currentUser.getEmail());
        application.put("licenseNumber", licenseNumber);
        application.put("status", "pending"); // <--- THIS IS ALREADY CORRECTLY SET TO "pending"
        application.put("timestamp", System.currentTimeMillis());

        // This saves the application to the 'doctorApplications' collection.
        db.collection("doctorApplications").document(currentUser.getUid())
                .set(application)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBarApplication.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(DoctorApplicationActivity.this, "Application submitted successfully! Please wait for admin review.", Toast.LENGTH_LONG).show();
                            finish(); // Close this activity
                        } else {
                            Toast.makeText(DoctorApplicationActivity.this, "Failed to submit application: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}