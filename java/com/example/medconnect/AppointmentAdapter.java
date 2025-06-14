package com.example.medconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    public interface OnAppointmentActionListener {
        void onAcceptAppointmentClick(Appointment appointment);
        void onRejectAppointmentClick(Appointment appointment);
        void onJoinMeetingClick(Appointment appointment);
    }

    private List<Appointment> appointmentList;
    private OnAppointmentActionListener listener;

    public AppointmentAdapter(List<Appointment> appointmentList, OnAppointmentActionListener listener) {
        this.appointmentList = appointmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);

        // Populate TextViews. Line 51 is likely one of these setText calls
        holder.textViewPatientName.setText("Patient: " + (appointment.getPatientName() != null ? appointment.getPatientName() : "N/A Patient"));
        holder.textViewPatientEmail.setText("Email: " + (appointment.getPatientEmail() != null ? appointment.getPatientEmail() : "N/A"));
        holder.textViewAppointmentDate.setText("Date: " + (appointment.getDate() != null ? appointment.getDate() : "N/A Date"));
        holder.textViewAppointmentTime.setText("Time: " + (appointment.getTime() != null ? appointment.getTime() : "N/A Time"));
        holder.textViewAppointmentType.setText("Type: " + (appointment.getType() != null ? appointment.getType() : "N/A Type"));
        holder.textViewAppointmentStatus.setText("Status: " + (appointment.getStatus() != null ? appointment.getStatus() : "N/A Status"));
        holder.textViewPaymentStatus.setText("Payment: " + (appointment.getPaymentStatus() != null ? appointment.getPaymentStatus() : "N/A"));
        holder.textViewAppointmentFee.setText(String.format(Locale.getDefault(), "Fee: ₹%.2f", appointment.getFee())); // Fee should always be a number


        // --- Conditional Visibility for Buttons (Doctor's side) ---
        // Reset visibility for all buttons to GONE initially
        holder.buttonAccept.setVisibility(View.GONE);
        holder.buttonReject.setVisibility(View.GONE);
        holder.buttonJoinMeeting.setVisibility(View.GONE);

        String status = appointment.getStatus();
        String type = appointment.getType();
        String paymentStatus = appointment.getPaymentStatus();
        String meetingLink = appointment.getMeetingLink();

        // Show Accept/Reject only if appointment is pending
        if (status != null && status.equalsIgnoreCase("pending")) {
            holder.buttonAccept.setVisibility(View.VISIBLE);
            holder.buttonReject.setVisibility(View.VISIBLE);
        }

        // Show Join Meeting only if accepted, paid, and video consultation with link
        if (status != null && status.equalsIgnoreCase("accepted") &&
                paymentStatus != null && paymentStatus.equalsIgnoreCase("completed") && // Assuming doctors only join paid calls
                type != null && type.equalsIgnoreCase("Video Consultation") &&
                meetingLink != null && !meetingLink.isEmpty()) {
            holder.buttonJoinMeeting.setVisibility(View.VISIBLE);
        }


        // Set OnClickListeners
        holder.buttonAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAcceptAppointmentClick(appointment);
            }
        });

        holder.buttonReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRejectAppointmentClick(appointment);
            }
        });

        holder.buttonJoinMeeting.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJoinMeetingClick(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPatientName;
        TextView textViewPatientEmail;
        TextView textViewAppointmentDate;
        TextView textViewAppointmentTime;
        TextView textViewAppointmentType;
        TextView textViewAppointmentStatus;
        TextView textViewPaymentStatus;
        TextView textViewAppointmentFee;
        Button buttonAccept;
        Button buttonReject;
        Button buttonJoinMeeting;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize all TextViews and Buttons from item_appointment.xml
            textViewPatientName = itemView.findViewById(R.id.textViewPatientName);
            textViewPatientEmail = itemView.findViewById(R.id.textViewPatientEmail);
            textViewAppointmentDate = itemView.findViewById(R.id.textViewAppointmentDate);
            textViewAppointmentTime = itemView.findViewById(R.id.textViewAppointmentTime);
            textViewAppointmentType = itemView.findViewById(R.id.textViewAppointmentType);
            textViewAppointmentStatus = itemView.findViewById(R.id.textViewAppointmentStatus);
            textViewPaymentStatus = itemView.findViewById(R.id.textViewPaymentStatus);
            textViewAppointmentFee = itemView.findViewById(R.id.textViewAppointmentFee);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonReject = itemView.findViewById(R.id.buttonReject);
            buttonJoinMeeting = itemView.findViewById(R.id.buttonJoinMeeting);
        }
    }
}