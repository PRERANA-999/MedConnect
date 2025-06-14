package com.example.medconnect;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // For loading images
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// For new ActivityResultLauncher for image picking
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


public class PatientProfileActivity extends AppCompatActivity {

    private ImageView imageViewProfilePic;
    private FloatingActionButton fabChangeProfilePic;
    private TextInputEditText etName, etEmail, etPhone, etDob, etAddress;
    private AutoCompleteTextView actvGender;
    private MaterialButton btnSaveChanges;
    private TextView tvNoPrescriptions;
    private RecyclerView recyclerViewPrescriptions;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String patientId;

    private Uri selectedImageUri; // Uri for the newly selected image

    // For picking image from gallery
    private ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageViewProfilePic.setImageURI(uri); // Display selected image
                        Toast.makeText(PatientProfileActivity.this, "Image selected. Click Save Changes to upload.", Toast.LENGTH_LONG).show();
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_profile);

        Toolbar toolbar = findViewById(R.id.toolbarProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if not logged in
            return;
        }
        patientId = currentUser.getUid();

        // Initialize UI elements
        imageViewProfilePic = findViewById(R.id.imageViewProfilePic);
        fabChangeProfilePic = findViewById(R.id.fabChangeProfilePic);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDob = findViewById(R.id.etDob);
        actvGender = findViewById(R.id.actvGender);
        etAddress = findViewById(R.id.etAddress);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        tvNoPrescriptions = findViewById(R.id.tvNoPrescriptions);
        recyclerViewPrescriptions = findViewById(R.id.recyclerViewPrescriptions);

        recyclerViewPrescriptions.setLayoutManager(new LinearLayoutManager(this));
        // You'll need to create a PrescriptionAdapter and Prescription model class later
        // recyclerViewPrescriptions.setAdapter(new PrescriptionAdapter(new ArrayList<>()));

        // Set up Gender Dropdown
        String[] genders = getResources().getStringArray(R.array.genders_array); // Define this in strings.xml
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        actvGender.setAdapter(genderAdapter);

        // Set up DOB DatePicker
        etDob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Load existing patient data
        loadPatientData();

        // Handle profile picture change FAB click
        fabChangeProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        // Handle Save Changes button click
        btnSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePatientData();
            }
        });
    }

    // Handle back arrow in Toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        if (!TextUtils.isEmpty(etDob.getText())) {
            try {
                // Try to parse existing date if any
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(etDob.getText().toString());
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {
                // Ignore parsing errors, use current date
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
                        etDob.setText(formattedDate);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }


    private void loadPatientData() {
        DocumentReference patientRef = db.collection("users").document(patientId);
        patientRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Populate UI fields
                        etName.setText(document.getString("name"));
                        etEmail.setText(document.getString("email")); // Email from Firebase Auth is also accessible via mAuth.getCurrentUser().getEmail()
                        etPhone.setText(document.getString("phone"));
                        etDob.setText(document.getString("dob"));
                        actvGender.setText(document.getString("gender"), false); // 'false' to not filter suggestions
                        etAddress.setText(document.getString("address"));

                        // Load profile picture using Glide
                        String profileImageUrl = document.getString("profileImageUrl");
                        if (!TextUtils.isEmpty(profileImageUrl)) {
                            Glide.with(PatientProfileActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_profile_placeholder) // Placeholder image
                                    .error(R.drawable.ic_profile_placeholder)       // Error image
                                    .into(imageViewProfilePic);
                        }
                    } else {
                        Toast.makeText(PatientProfileActivity.this, "Patient data not found in Firestore. Creating new profile.", Toast.LENGTH_SHORT).show();
                        // Populate with email from Firebase Auth if document doesn't exist yet
                        if (mAuth.getCurrentUser() != null) {
                            etEmail.setText(mAuth.getCurrentUser().getEmail());
                        }
                    }
                } else {
                    Toast.makeText(PatientProfileActivity.this, "Failed to load profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load prescriptions
        loadPrescriptions();
    }

    private void savePatientData() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String gender = actvGender.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(dob) || TextUtils.isEmpty(gender) || TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data for Firestore
        Map<String, Object> patientData = new java.util.HashMap<>();
        patientData.put("name", name);
        patientData.put("phone", phone);
        patientData.put("dob", dob);
        patientData.put("gender", gender);
        patientData.put("address", address);
        // Do NOT update email here; it's handled by Firebase Authentication.

        // Update Firestore document
        DocumentReference patientRef = db.collection("users").document(patientId);

        // First upload image if a new one was selected
        if (selectedImageUri != null) {
            uploadProfileImage(patientRef, patientData);
        } else {
            // No new image selected, just update other data
            updateFirestoreData(patientRef, patientData);
        }
    }

    private void openImageChooser() {
        mGetContent.launch("image/*"); // Allows picking any image type
    }

    private void uploadProfileImage(final DocumentReference patientRef, final Map<String, Object> patientData) {
        if (selectedImageUri != null) {
            StorageReference profileImageRef = storageRef.child("profile_images/" + patientId + ".jpg");
            profileImageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            patientData.put("profileImageUrl", imageUrl);
                            updateFirestoreData(patientRef, patientData); // Save other data with image URL
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(PatientProfileActivity.this, "Failed to upload profile image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateFirestoreData(patientRef, patientData); // Save other data even if image upload fails
                    });
        }
    }

    private void updateFirestoreData(DocumentReference patientRef, Map<String, Object> patientData) {
        patientRef.update(patientData)
                .addOnSuccessListener(aVoid -> Toast.makeText(PatientProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(PatientProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadPrescriptions() {
        // Clear previous prescriptions
        List<Prescription> prescriptionList = new ArrayList<>();
        // In a real app, you'd fetch from a 'prescriptions' subcollection or a 'prescriptions' collection
        // with queries. For now, let's simulate or fetch a simple list.

        // Example: Fetching from a subcollection users/{patientId}/prescriptions
        db.collection("users").document(patientId).collection("prescriptions")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING) // Order by date
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Convert Firestore document to Prescription object
                            // You will need a Prescription model class
                            // Example: Prescription prescription = document.toObject(Prescription.class);
                            // prescriptionList.add(prescription);

                            // For now, let's just display basic info if no Prescription class exists yet
                            String doctorName = document.getString("doctorName");
                            String diagnosis = document.getString("diagnosis");
                            String date = document.getString("date"); // Assuming date is stored as YYYY-MM-DD
                            List<Map<String, String>> medications = (List<Map<String, String>>) document.get("medications");

                            StringBuilder medsBuilder = new StringBuilder();
                            if (medications != null) {
                                for (Map<String, String> med : medications) {
                                    medsBuilder.append("- ").append(med.get("name")).append(" (").append(med.get("dosage")).append(")\n");
                                }
                            }

                            // Create a simple Prescription object or just display
                            // This is a placeholder until you define a proper Prescription class and Adapter
                            Prescription dummyPrescription = new Prescription(
                                    document.getId(), doctorName, diagnosis, date, medsBuilder.toString()
                            );
                            prescriptionList.add(dummyPrescription);
                        }

                        if (prescriptionList.isEmpty()) {
                            tvNoPrescriptions.setVisibility(View.VISIBLE);
                            recyclerViewPrescriptions.setVisibility(View.GONE);
                        } else {
                            tvNoPrescriptions.setVisibility(View.GONE);
                            recyclerViewPrescriptions.setVisibility(View.VISIBLE);
                            // Set adapter for RecyclerView
                            recyclerViewPrescriptions.setAdapter(new PrescriptionAdapter(prescriptionList));
                        }
                    } else {
                        Toast.makeText(PatientProfileActivity.this, "Error getting prescriptions: " + task.getException(), Toast.LENGTH_SHORT).show();
                        tvNoPrescriptions.setVisibility(View.VISIBLE);
                        recyclerViewPrescriptions.setVisibility(View.GONE);
                    }
                });
    }

    // --- Prescription Model Class (Add this as a new file: Prescription.java) ---
    public static class Prescription {
        public String id;
        public String doctorName;
        public String diagnosis;
        public String date;
        public String medicationsSummary; // Simplified for display

        public Prescription() {
            // Public no-arg constructor needed for Firestore
        }

        public Prescription(String id, String doctorName, String diagnosis, String date, String medicationsSummary) {
            this.id = id;
            this.doctorName = doctorName;
            this.diagnosis = diagnosis;
            this.date = date;
            this.medicationsSummary = medicationsSummary;
        }

        // Getters (needed for Firestore toObject() and RecyclerView adapter)
        public String getId() { return id; }
        public String getDoctorName() { return doctorName; }
        public String getDiagnosis() { return diagnosis; }
        public String getDate() { return date; }
        public String getMedicationsSummary() { return medicationsSummary; }
    }

    // --- Prescription Adapter Class (Add this as a new file: PrescriptionAdapter.java) ---
    // This will handle how each prescription item looks in the RecyclerView
    // You'll also need to create a layout file for each item: item_prescription.xml
    public static class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.PrescriptionViewHolder> {

        private List<Prescription> prescriptionList;

        public PrescriptionAdapter(List<Prescription> prescriptionList) {
            this.prescriptionList = prescriptionList;
        }

        @NonNull
        @Override
        public PrescriptionViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription, parent, false);
            return new PrescriptionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PrescriptionViewHolder holder, int position) {
            Prescription prescription = prescriptionList.get(position);
            holder.tvDoctorName.setText("Dr. " + prescription.getDoctorName());
            holder.tvDate.setText(prescription.getDate());
            holder.tvDiagnosis.setText("Diagnosis: " + prescription.getDiagnosis());
            holder.tvMedications.setText("Medications:\n" + prescription.getMedicationsSummary());
        }

        @Override
        public int getItemCount() {
            return prescriptionList.size();
        }

        static class PrescriptionViewHolder extends RecyclerView.ViewHolder {
            TextView tvDoctorName, tvDate, tvDiagnosis, tvMedications;

            public PrescriptionViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDoctorName = itemView.findViewById(R.id.tvPrescriptionDoctorName);
                tvDate = itemView.findViewById(R.id.tvPrescriptionDate);
                tvDiagnosis = itemView.findViewById(R.id.tvPrescriptionDiagnosis);
                tvMedications = itemView.findViewById(R.id.tvPrescriptionMedications);
            }
        }
    }
}