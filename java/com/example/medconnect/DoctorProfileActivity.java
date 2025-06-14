package com.example.medconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DoctorProfileActivity extends AppCompatActivity {

    private TextView doctorEmailDisplay;
    // NEW: EditText for Username
    private EditText editTextUsername;
    private EditText editTextSpecialization, editTextConsultationFee;
    private EditText editTextPhoneNumber, editTextClinicAddress, editTextYearsExperience;

    private Button buttonSaveProfile;
    private Button buttonEditProfile;
    private ProgressBar progressBarProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        doctorEmailDisplay = findViewById(R.id.doctorEmailDisplay);
        // Initialize NEW EditText for Username
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextSpecialization = findViewById(R.id.editTextSpecialization);
        editTextConsultationFee = findViewById(R.id.editTextConsultationFee);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextClinicAddress = findViewById(R.id.editTextClinicAddress);
        editTextYearsExperience = findViewById(R.id.editTextYearsExperience);

        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);
        buttonEditProfile = findViewById(R.id.buttonEditProfile);
        progressBarProfile = findViewById(R.id.progressBarProfile);

        if (currentUser == null) {
            Toast.makeText(this, "Doctor not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        doctorEmailDisplay.setText("Email: " + currentUser.getEmail());

        // Initially disable all editable fields
        setEditableFieldsEnabled(false);
        buttonSaveProfile.setVisibility(View.GONE);
        buttonEditProfile.setVisibility(View.VISIBLE);

        loadDoctorProfile(); // Load existing data on creation

        buttonSaveProfile.setOnClickListener(v -> saveDoctorProfile());
        buttonEditProfile.setOnClickListener(v -> toggleEditMode(true));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Helper method to enable/disable editable fields
    private void setEditableFieldsEnabled(boolean enabled) {
        editTextUsername.setEnabled(enabled); // NEW: Enable/disable username field
        editTextSpecialization.setEnabled(enabled);
        editTextConsultationFee.setEnabled(enabled);
        editTextPhoneNumber.setEnabled(enabled);
        editTextClinicAddress.setEnabled(enabled);
        editTextYearsExperience.setEnabled(enabled);
    }

    // Helper method to toggle edit mode (no change needed here beyond setEditableFieldsEnabled)
    private void toggleEditMode(boolean enable) {
        setEditableFieldsEnabled(enable);
        if (enable) {
            buttonEditProfile.setVisibility(View.GONE);
            buttonSaveProfile.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Editing mode enabled.", Toast.LENGTH_SHORT).show();
        } else {
            buttonEditProfile.setVisibility(View.VISIBLE);
            buttonSaveProfile.setVisibility(View.GONE);
            Toast.makeText(this, "Editing mode disabled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDoctorProfile() {
        progressBarProfile.setVisibility(View.VISIBLE);
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    progressBarProfile.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Populate NEW username field
                            String username = document.getString("username");
                            if (username != null) {
                                editTextUsername.setText(username);
                            }

                            String specialization = document.getString("specialization");
                            Double consultationFee = document.getDouble("consultationFee");
                            String phoneNumber = document.getString("phoneNumber");
                            String clinicAddress = document.getString("clinicAddress");
                            Long yearsExperience = document.getLong("yearsExperience");

                            if (specialization != null) {
                                editTextSpecialization.setText(specialization);
                            }
                            if (consultationFee != null) {
                                editTextConsultationFee.setText(String.valueOf(consultationFee));
                            }
                            if (phoneNumber != null) {
                                editTextPhoneNumber.setText(phoneNumber);
                            }
                            if (clinicAddress != null) {
                                editTextClinicAddress.setText(clinicAddress);
                            }
                            if (yearsExperience != null) {
                                editTextYearsExperience.setText(String.valueOf(yearsExperience));
                            }

                            Toast.makeText(DoctorProfileActivity.this, "Profile loaded.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(DoctorProfileActivity.this, "No profile data found. Please save.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(DoctorProfileActivity.this, "Error loading profile: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveDoctorProfile() {
        String username = editTextUsername.getText().toString().trim(); // NEW: Get username
        String specialization = editTextSpecialization.getText().toString().trim();
        String feeString = editTextConsultationFee.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String clinicAddress = editTextClinicAddress.getText().toString().trim();
        String yearsExperienceString = editTextYearsExperience.getText().toString().trim();

        // Basic validation for username (optional, but good practice)
        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("Username is required.");
            // Or, allow empty username if it's optional:
            // Toast.makeText(this, "Username is empty.", Toast.LENGTH_SHORT).show();
            // username = null; // Store null in Firestore if empty
            return;
        }


        if (TextUtils.isEmpty(specialization)) {
            editTextSpecialization.setError("Specialization is required.");
            return;
        }
        if (TextUtils.isEmpty(feeString)) {
            editTextConsultationFee.setError("Consultation Fee is required.");
            return;
        }

        double consultationFee;
        try {
            consultationFee = Double.parseDouble(feeString);
            if (consultationFee <= 0) {
                editTextConsultationFee.setError("Fee must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            editTextConsultationFee.setError("Invalid fee format.");
            return;
        }

        Integer yearsExperience = null;
        if (!TextUtils.isEmpty(yearsExperienceString)) {
            try {
                yearsExperience = Integer.parseInt(yearsExperienceString);
                if (yearsExperience < 0) {
                    editTextYearsExperience.setError("Years must be non-negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                editTextYearsExperience.setError("Invalid years format.");
                return;
            }
        }

        progressBarProfile.setVisibility(View.VISIBLE);

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("username", username); // NEW: Save username
        profileUpdates.put("specialization", specialization);
        profileUpdates.put("consultationFee", consultationFee);
        profileUpdates.put("phoneNumber", phoneNumber);
        profileUpdates.put("clinicAddress", clinicAddress);
        profileUpdates.put("yearsExperience", yearsExperience);

        db.collection("users").document(currentUser.getUid())
                .update(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    progressBarProfile.setVisibility(View.GONE);
                    Toast.makeText(DoctorProfileActivity.this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    toggleEditMode(false);
                })
                .addOnFailureListener(e -> {
                    progressBarProfile.setVisibility(View.GONE);
                    Toast.makeText(DoctorProfileActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}