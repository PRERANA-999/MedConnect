package com.example.medconnect;

public class Doctor {
    private String uid;
    private String email;
    private String name; // Assuming you'll add doctor's name later
    private String specialization;
    private String licenseNumber;
    private String role;
    private String status;
    private String username; // NEW FIELD: username

    public Doctor() {
        // No-argument constructor required for Firestore
    }

    public Doctor(String uid, String email, String name, String specialization, String licenseNumber, String role, String status, String username) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.specialization = specialization;
        this.licenseNumber = licenseNumber;
        this.role = role;
        this.status = status;
        this.username = username; // Initialize new field
    }

    // Getters
    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getUsername() { // NEW GETTER
        return username;
    }

    // Setters (if needed for data manipulation, but Firestore usually just uses getters for retrieval)
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUsername(String username) { // NEW SETTER
        this.username = username;
    }
}