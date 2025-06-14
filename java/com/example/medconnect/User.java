package com.example.medconnect;

import com.google.firebase.firestore.PropertyName; // Import this

public class User {
    private String uid;
    @PropertyName("username") // Map Firestore's 'username' to this field
    private String name;
    private String email;
    private String role;
    @PropertyName("specialization") // Map Firestore's 'specialization' to this field
    private String qualification;
    @PropertyName("phoneNumber") // Map Firestore's 'phoneNumber' to this field
    private String phone;

    private String clinicAddress;
    private Long consultationFee;
    private Boolean firstLoginDone;
    private String licenseNumber;
    private Long yearsExperience;

    public User() {
        // Public no-argument constructor needed for Firestore's .toObject(User.class)
    }

    // Standard getters and setters for 'name'
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Standard getters and setters for 'email'
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Standard getters and setters for 'role'
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Standard getters and setters for 'qualification' (maps to 'specialization' in Firestore)
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    // Standard getters and setters for 'phone' (maps to 'phoneNumber' in Firestore)
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // Add getters and setters for all other fields as you already have:
    public String getClinicAddress() { return clinicAddress; }
    public void setClinicAddress(String clinicAddress) { this.clinicAddress = clinicAddress; }

    public Long getConsultationFee() { return consultationFee; }
    public void setConsultationFee(Long consultationFee) { this.consultationFee = consultationFee; }

    public Boolean getFirstLoginDone() { return firstLoginDone; }
    public void setFirstLoginDone(Boolean firstLoginDone) { this.firstLoginDone = firstLoginDone; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public Long getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Long yearsExperience) { this.yearsExperience = yearsExperience; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    // *** IMPORTANT: ADDED toString() METHOD FOR DEBUGGING ***
    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", qualification='" + qualification + '\'' +
                ", phone='" + phone + '\'' +
                ", clinicAddress='" + clinicAddress + '\'' +
                ", consultationFee=" + consultationFee +
                ", firstLoginDone=" + firstLoginDone +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", yearsExperience=" + yearsExperience +
                '}';
    }
}