package com.example.medconnect;

import android.content.Intent;
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
import com.google.firebase.firestore.SetOptions; // For merging data

import java.util.HashMap;
import java.util.Map;
import android.view.MenuItem;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editTextNewPassword, editTextConfirmPassword;
    private Button buttonChangePassword;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password); // Make sure this layout exists

        // If you have an action bar and want a back button:
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Set New Password");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        progressBar = findViewById(R.id.progressBar);

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            // Redirect to login if no user is found
            Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        buttonChangePassword.setOnClickListener(v -> changePassword());
    }

    // Handles back button press in action bar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Prevent going back to LoginActivity if forced password change
            Toast.makeText(this, "Please set your new password first.", Toast.LENGTH_SHORT).show();
            return true; // Consume the back press
        }
        return super.onOptionsItemSelected(item);
    }

    private void changePassword() {
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPassword)) {
            editTextNewPassword.setError("New Password is required.");
            return;
        }
        if (newPassword.length() < 6) {
            editTextNewPassword.setError("Password must be at least 6 characters.");
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.setError("Confirm Password is required.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Update password in Firebase Authentication
        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Mark first login as done in Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("firstLoginDone", true);

                        db.collection("users").document(currentUser.getUid())
                                .set(updates, SetOptions.merge()) // Use SetOptions.merge() to only update fields
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ChangePasswordActivity.this, "Password changed successfully! Welcome.", Toast.LENGTH_LONG).show();
                                    // Redirect to Doctor Dashboard
                                    Intent intent = new Intent(ChangePasswordActivity.this, DoctorDashboardActivity.class);
                                    startActivity(intent);
                                    finish(); // Prevent going back to this activity
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ChangePasswordActivity.this, "Password changed, but failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    // Even if Firestore update fails, password is changed, so redirect.
                                    Intent intent = new Intent(ChangePasswordActivity.this, DoctorDashboardActivity.class);
                                    startActivity(intent);
                                    finish();
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ChangePasswordActivity.this, "Failed to change password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}