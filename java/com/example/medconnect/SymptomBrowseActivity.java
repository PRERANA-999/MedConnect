package com.example.medconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SymptomBrowseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_browse);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // --- Handle Symptom Card Clicks ---
        findViewById(R.id.cardCough).setOnClickListener(v -> searchBySymptom("Cough"));
        findViewById(R.id.cardCold).setOnClickListener(v -> searchBySymptom("Cold"));
        findViewById(R.id.cardFever).setOnClickListener(v -> searchBySymptom("Fever"));
        findViewById(R.id.cardHeadache).setOnClickListener(v -> searchBySymptom("Headache"));
        findViewById(R.id.cardBodyPain).setOnClickListener(v -> searchBySymptom("Body Pain"));


        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            redirectUserBasedOnRole(currentUser.getUid());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavigationDrawerMenu();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            startActivity(new Intent(SymptomBrowseActivity.this, LoginActivity.class));
        } else if (id == R.id.nav_register) {
            startActivity(new Intent(SymptomBrowseActivity.this, RegisterActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
            updateNavigationDrawerMenu();
            // This re-launches SymptomBrowseActivity to ensure a clean unauthenticated state
            Intent intent = new Intent(SymptomBrowseActivity.this, SymptomBrowseActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish current instance
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateNavigationDrawerMenu() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigationView.getMenu().findItem(R.id.nav_login).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_register).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(true);
        } else {
            navigationView.getMenu().findItem(R.id.nav_login).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_register).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(false);
        }
    }

    private void redirectUserBasedOnRole(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Intent intent;
                        if ("patient".equals(role)) {
                            intent = new Intent(SymptomBrowseActivity.this, PatientDashboardActivity.class);
                        } else if ("doctor".equals(role)) {
                            intent = new Intent(SymptomBrowseActivity.this, DoctorDashboardActivity.class);
                        } else if ("admin".equals(role)) {
                            intent = new Intent(SymptomBrowseActivity.this, AdminDashboardActivity.class);
                        } else {
                            Toast.makeText(SymptomBrowseActivity.this, "Unknown role, please contact support.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            updateNavigationDrawerMenu();
                            return;
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SymptomBrowseActivity.this, "User data not found, please re-login.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        updateNavigationDrawerMenu();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SymptomBrowseActivity.this, "Failed to retrieve user role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                    updateNavigationDrawerMenu();
                });
    }

    // --- MODIFIED: Launch RemedyDetailActivity and pass symptom ---
    private void searchBySymptom(String symptom) {
        Intent intent = new Intent(SymptomBrowseActivity.this, RemedyDetailActivity.class);
        intent.putExtra(RemedyDetailActivity.EXTRA_SYMPTOM_NAME, symptom); // Pass the symptom name
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}