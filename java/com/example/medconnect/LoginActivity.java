package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private ProgressBar progressBar;
    private TextView textViewForgotPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        progressBar = findViewById(R.id.progressBar);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        buttonLogin.setOnClickListener(v -> loginUser());
        textViewRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        textViewForgotPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Please enter your email to reset password.", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required.");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required.");
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            fetchUserRoleAndRedirect(currentUser.getUid());
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this, "Login failed. User not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserRoleAndRedirect(String uid) {
        DocumentReference docRef = db.collection("users").document(uid);
        Log.d("FirestoreDebug", "Attempting to fetch role for UID: " + uid + " from Firestore.");

        docRef.get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.d("FirestoreDebug", "Firestore task successful. Document exists: " + document.exists());
                    Log.d("FirestoreDebug", "Document data: " + document.getData());

                    String role = document.getString("role");
                    Boolean firstLoginDone = document.getBoolean("firstLoginDone");
                    if (firstLoginDone == null) firstLoginDone = false; // Default to false if not set

                    Log.d("FirestoreDebug", "Role fetched: " + role);

                    if ("patient".equals(role)) {
                        // --- MODIFIED HERE: Redirect to the NEW PatientDashboardActivity (the menu) ---
                        Intent intent = new Intent(LoginActivity.this, PatientDashboardActivity.class);
                        startActivity(intent);
                        finish();
                        Toast.makeText(LoginActivity.this, "Logged in as Patient.", Toast.LENGTH_SHORT).show();
                    } else if ("doctor".equals(role)) {
                        String status = document.getString("status");

                        // If doctor's status is approved (or null, treating as approved for old docs)
                        if (status == null || "approved".equals(status)) {
                            // If it's a first login (admin created account)
                            if (firstLoginDone != null && !firstLoginDone) {
                                Toast.makeText(LoginActivity.this, "Please set a new password for your account.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(LoginActivity.this, ChangePasswordActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Doctor is approved and has already set a new password (or no flag means good)
                                Intent intent = new Intent(LoginActivity.this, DoctorDashboardActivity.class);
                                startActivity(intent);
                                finish();
                                Toast.makeText(LoginActivity.this, "Logged in as Doctor.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // If status is pending or rejected
                            Toast.makeText(LoginActivity.this, "Your doctor application is " + status + ". Please contact support.", Toast.LENGTH_LONG).show();
                            mAuth.signOut(); // Log out doctor if not approved
                            Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else if ("admin".equals(role)) {
                        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        startActivity(intent);
                        finish();
                        Toast.makeText(LoginActivity.this, "Logged in as Admin.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Unknown user role. Please contact support.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut(); // Sign out user with unknown role
                        Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "User data not found. Please complete registration.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut(); // No user document found for UID
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class); // Or back to login
                    startActivity(intent);
                    finish();
                }
            } else {
                Toast.makeText(LoginActivity.this, "Failed to retrieve user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching user role", task.getException());
                mAuth.signOut(); // Sign out on task failure
                Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserRoleAndRedirect(currentUser.getUid());
        }
    }
}