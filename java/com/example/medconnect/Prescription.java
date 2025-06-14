package com.example.medconnect;

// No specific imports are strictly needed for this basic data model
// If you were to store complex objects (like a list of medication objects)
// within the Prescription, you might need imports for List or Map.

public class Prescription {
    private String id; // Unique ID for the prescription document
    private String doctorName;
    private String diagnosis;
    private String date; // E.g., "YYYY-MM-DD"
    private String medicationsSummary; // A summary string of medications for display
    private String attachmentUrl;      // URL to the attached prescription file (PDF, JPG, etc.)

    // Required public no-argument constructor for Firebase Firestore toObject()
    public Prescription() {
    }

    // Constructor with all fields
    public Prescription(String id, String doctorName, String diagnosis, String date, String medicationsSummary, String attachmentUrl) {
        this.id = id;
        this.doctorName = doctorName;
        this.diagnosis = diagnosis;
        this.date = date;
        this.medicationsSummary = medicationsSummary;
        this.attachmentUrl = attachmentUrl;
    }

    // --- Getters for all fields (needed by Firestore and RecyclerView Adapter) ---
    public String getId() {
        return id;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getDate() {
        return date;
    }

    public String getMedicationsSummary() {
        return medicationsSummary;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    // --- Setters (optional, but good for flexibility if you modify data) ---
    // If you plan to update Prescription objects, you might need setters.
    // Firestore's toObject() usually works with public fields or getters,
    // but for manual data manipulation or updates, setters are useful.

    public void setId(String id) {
        this.id = id;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setMedicationsSummary(String medicationsSummary) {
        this.medicationsSummary = medicationsSummary;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
}