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

public class PatientDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String patientId; // To store the logged-in patient's UID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard); // Correct layout for this activity

        Toolbar toolbar = findViewById(R.id.toolbar_patient_dashboard);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout_patient_dashboard);
        navigationView = findViewById(R.id.nav_view_patient_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If somehow not logged in, redirect to login
            startActivity(new Intent(PatientDashboardActivity.this, LoginActivity.class));
            finish();
            return;
        }
        patientId = currentUser.getUid();


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        updateNavigationDrawerMenu(); // Update drawer visibility based on login status

        // --- Handle Symptom Card Clicks (Same as SymptomBrowseActivity) ---
        findViewById(R.id.cardCoughPatient).setOnClickListener(v -> searchBySymptom("Cough"));
        findViewById(R.id.cardColdPatient).setOnClickListener(v -> searchBySymptom("Cold"));
        findViewById(R.id.cardFeverPatient).setOnClickListener(v -> searchBySymptom("Fever"));
        findViewById(R.id.cardHeadachePatient).setOnClickListener(v -> searchBySymptom("Headache"));
        findViewById(R.id.cardBodyPainPatient).setOnClickListener(v -> searchBySymptom("Body Pain"));

        // --- Handle Bottom Navigation Button Clicks ---
        findViewById(R.id.btnViewDoctors).setOnClickListener(v -> {
            startActivity(new Intent(PatientDashboardActivity.this, DoctorListActivity.class));
        });

        findViewById(R.id.btnMyAppointments).setOnClickListener(v -> {
            startActivity(new Intent(PatientDashboardActivity.this, PatientAppointmentsActivity.class));
        });

        findViewById(R.id.btnMyProfile).setOnClickListener(v -> {
            // Toast.makeText(this, "Opening Patient Profile...", Toast.LENGTH_SHORT).show(); // Remove this Toast
            startActivity(new Intent(PatientDashboardActivity.this, PatientProfileActivity.class));
        });

        findViewById(R.id.btnLogoutPatientDashboard).setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
            // Redirect to SymptomBrowseActivity or LoginActivity after logout
            Intent intent = new Intent(PatientDashboardActivity.this, SymptomBrowseActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            startActivity(new Intent(PatientDashboardActivity.this, LoginActivity.class));
        } else if (id == R.id.nav_register) {
            startActivity(new Intent(PatientDashboardActivity.this, RegisterActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
            updateNavigationDrawerMenu(); // Update drawer after logout
            Intent intent = new Intent(PatientDashboardActivity.this, SymptomBrowseActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_about_us) {
            // UNCOMMENT THE LINE BELOW AND REMOVE THE TOAST
            startActivity(new Intent(PatientDashboardActivity.this, AboutUsActivity.class));
            // Toast.makeText(this, "Opening About Us...", Toast.LENGTH_SHORT).show(); // REMOVE THIS LINE
        } else if (id == R.id.nav_contact_us) {
            // UNCOMMENT THE LINE BELOW AND REMOVE THE TOAST
            startActivity(new Intent(PatientDashboardActivity.this, ContactUsActivity.class));
            // Toast.makeText(this, "Opening Contact Us...", Toast.LENGTH_SHORT).show(); // REMOVE THIS LINE
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // Helper method to launch RemedyDetailActivity
    private void searchBySymptom(String symptom) {
        Intent intent = new Intent(PatientDashboardActivity.this, RemedyDetailActivity.class);
        intent.putExtra(RemedyDetailActivity.EXTRA_SYMPTOM_NAME, symptom);
        startActivity(intent);
    }

    // Update navigation drawer items visibility based on login status
    private void updateNavigationDrawerMenu() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigationView.getMenu().findItem(R.id.nav_login).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_register).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(true);
            // If you have "My Profile" or other patient-specific items in the drawer, make them visible here.
        } else {
            navigationView.getMenu().findItem(R.id.nav_login).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_register).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(false);
        }
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