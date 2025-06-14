package com.example.medconnect;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date; // <--- IMPORTANT: Ensure this is java.util.Date

public class Appointment {

    @DocumentId
    private String appointmentId;

    private String patientUid;
    private String patientName;
    private String patientEmail;
    private String doctorUid;
    private String doctorName;
    private String doctorEmail;
    private String doctorSpecialization;
    private String date;
    private String time;
    private String type;
    private String status;
    private String paymentStatus;
    private double fee;
    private String meetingLink;
    private String meetingId;
    private Date timestamp; // <--- MAKE SURE THIS IS Date
    private boolean isMeetingLinkAccessed; // <-- ADD THIS LINE

    private String transactionId;

    public Appointment() {
        this.paymentStatus = "pending";
    }

    // Constructor
    public Appointment(String patientUid, String patientName, String patientEmail,
                       String doctorUid, String doctorName, String doctorEmail,
                       String doctorSpecialization, String date, String time,
                       String type, String status, String paymentStatus, double fee,
                       String meetingLink, String meetingId, Date timestamp, // <--- MAKE SURE THIS IS Date
                       String transactionId) {
        this.patientUid = patientUid;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.doctorUid = doctorUid;
        this.doctorName = doctorName;
        this.doctorEmail = doctorEmail;
        this.doctorSpecialization = doctorSpecialization;
        this.date = date;
        this.time = time;
        this.type = type;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.fee = fee;
        this.meetingLink = meetingLink;
        this.meetingId = meetingId;
        this.timestamp = timestamp;
        this.transactionId = transactionId;
    }

    // Getters and Setters
    // (All other getters and setters remain as you provided)

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public void setPatientEmail(String patientEmail) {
        this.patientEmail = patientEmail;
    }

    public String getDoctorUid() {
        return doctorUid;
    }

    public void setDoctorUid(String doctorUid) {
        this.doctorUid = doctorUid;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorEmail() {
        return doctorEmail;
    }

    public void setDoctorEmail(String doctorEmail) {
        this.doctorEmail = doctorEmail;
    }

    public String getDoctorSpecialization() {
        return doctorSpecialization;
    }

    public void setDoctorSpecialization(String doctorSpecialization) {
        this.doctorSpecialization = doctorSpecialization;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public Date getTimestamp() {
        return timestamp;
    } // <--- Getter for Date

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    } // <--- Setter for Date

//    public boolean getIsMeetingLinkAccessed() { // Note: Firestore often prefers 'getIsField' for booleans
//        return isMeetingLinkAccessed;
//    }
//
//    public void setIsMeetingLinkAccessed(boolean meetingLinkAccessed) {
//        isMeetingLinkAccessed = meetingLinkAccessed;
//    }
}