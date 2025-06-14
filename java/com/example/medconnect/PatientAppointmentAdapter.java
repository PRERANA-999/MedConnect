package com.example.medconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale; // For String.format

public class PatientAppointmentAdapter extends RecyclerView.Adapter<PatientAppointmentAdapter.PatientAppointmentViewHolder> {

    // Updated interface to include the Cancel action
    public interface OnAppointmentActionListener {
        void onPayNowClick(Appointment appointment);
        void onJoinMeetingClick(Appointment appointment);
        void onCancelAppointmentClick(Appointment appointment); // Added for the cancel button
    }

    private List<Appointment> appointmentList;
    private OnAppointmentActionListener listener;

    public PatientAppointmentAdapter(List<Appointment> appointmentList, OnAppointmentActionListener listener) {
        this.appointmentList = appointmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PatientAppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_appointment, parent, false);
        return new PatientAppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientAppointmentViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);

        // Populate TextViews with null checks for safety
        holder.textViewDoctorName.setText("Doctor: " + (appointment.getDoctorName() != null ? appointment.getDoctorName() : "N/A Doctor"));
        holder.textViewAppointmentDate.setText("Date: " + (appointment.getDate() != null ? appointment.getDate() : "N/A Date"));
        holder.textViewAppointmentTime.setText("Time: " + (appointment.getTime() != null ? appointment.getTime() : "N/A Time"));
        holder.textViewAppointmentType.setText("Type: " + (appointment.getType() != null ? appointment.getType() : "N/A Type"));
        holder.textViewAppointmentFee.setText(String.format(Locale.getDefault(), "Fee: ₹%.2f", appointment.getFee())); // Fee should always be a number

        String status = appointment.getStatus();
        String paymentStatus = appointment.getPaymentStatus();
        String transactionId = appointment.getTransactionId();
        String type = appointment.getType();
        String meetingLink = appointment.getMeetingLink();

        holder.textViewAppointmentStatus.setText("Status: " + (status != null ? status : "N/A Status"));
        holder.textViewPaymentStatus.setText("Payment Status: " + (paymentStatus != null ? paymentStatus : "N/A"));

        // Display Transaction ID conditionally
        if (transactionId != null && !transactionId.isEmpty() && !"N/A".equalsIgnoreCase(transactionId)) {
            holder.textViewTransactionId.setText("Txn ID: " + transactionId);
            holder.textViewTransactionId.setVisibility(View.VISIBLE);
        } else {
            holder.textViewTransactionId.setVisibility(View.GONE);
        }

        // --- Conditional Visibility for Buttons ---
        // Reset visibility for all buttons to GONE initially
        holder.buttonPayNow.setVisibility(View.GONE);
        holder.buttonPatientJoinMeeting.setVisibility(View.GONE);
        holder.buttonPatientCancelAppointment.setVisibility(View.GONE);


        // Logic for "Pay Now" button
        if (status != null && status.equalsIgnoreCase("accepted") &&
                paymentStatus != null && paymentStatus.equalsIgnoreCase("pending")) {
            holder.buttonPayNow.setVisibility(View.VISIBLE);
            holder.buttonPayNow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPayNowClick(appointment);
                }
            });
        }

        // Logic for "Join Meeting" button for patient
        if (status != null && status.equalsIgnoreCase("accepted") &&
                paymentStatus != null && paymentStatus.equalsIgnoreCase("completed") &&
                type != null && type.equalsIgnoreCase("Video Consultation") &&
                meetingLink != null && !meetingLink.isEmpty()) {
            holder.buttonPatientJoinMeeting.setVisibility(View.VISIBLE);
            holder.buttonPatientJoinMeeting.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onJoinMeetingClick(appointment);
                }
            });
        }

        // Logic for "Cancel Appointment" button for patient
        // Allow cancellation if not already cancelled, rejected, or completed.
        // It's common to allow cancellation if status is pending or accepted.
        if (status != null &&
                !status.equalsIgnoreCase("cancelled") &&
                !status.equalsIgnoreCase("rejected") &&
                !status.equalsIgnoreCase("completed")) {
            holder.buttonPatientCancelAppointment.setVisibility(View.VISIBLE);
            holder.buttonPatientCancelAppointment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancelAppointmentClick(appointment);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    static class PatientAppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDoctorName;
        TextView textViewAppointmentDate;
        TextView textViewAppointmentTime;
        TextView textViewAppointmentType;
        TextView textViewAppointmentFee;
        TextView textViewAppointmentStatus;
        TextView textViewPaymentStatus;
        TextView textViewTransactionId; // Added for transaction ID
        Button buttonPayNow;
        Button buttonPatientJoinMeeting;
        Button buttonPatientCancelAppointment; // Added for Cancel button

        public PatientAppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDoctorName = itemView.findViewById(R.id.textViewDoctorName);
            textViewAppointmentDate = itemView.findViewById(R.id.textViewAppointmentDate);
            textViewAppointmentTime = itemView.findViewById(R.id.textViewAppointmentTime);
            textViewAppointmentType = itemView.findViewById(R.id.textViewAppointmentType);
            textViewAppointmentFee = itemView.findViewById(R.id.textViewAppointmentFee);
            textViewAppointmentStatus = itemView.findViewById(R.id.textViewAppointmentStatus);
            textViewPaymentStatus = itemView.findViewById(R.id.textViewPaymentStatus);
            textViewTransactionId = itemView.findViewById(R.id.textViewTransactionId); // Initialize Txn ID TextView
            buttonPayNow = itemView.findViewById(R.id.buttonPayNow);
            buttonPatientJoinMeeting = itemView.findViewById(R.id.buttonPatientJoinMeeting);
            buttonPatientCancelAppointment = itemView.findViewById(R.id.buttonPatientCancelAppointment); // Initialize Cancel button
        }
    }
}