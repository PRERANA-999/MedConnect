package com.example.medconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private List<Doctor> doctorList;
    private OnDoctorClickListener listener;

    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor doctor);
    }

    public DoctorAdapter(List<Doctor> doctorList, OnDoctorClickListener listener) {
        this.doctorList = doctorList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        Doctor doctor = doctorList.get(position);

        // Display username if available, otherwise email
        if (doctor.getUsername() != null && !doctor.getUsername().isEmpty()) {
            holder.textViewDoctorName.setText("Dr. " + doctor.getUsername());
        } else {
            holder.textViewDoctorName.setText("Dr. " + doctor.getEmail()); // Fallback to email
        }

        // Display specialization
        holder.textViewDoctorSpecialization.setText(doctor.getSpecialization() != null && !doctor.getSpecialization().isEmpty() ?
                "Specialization: " + doctor.getSpecialization() : "Specialization: Not specified");


        holder.buttonBookAppointment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDoctorClick(doctor);
            }
        });
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
    }

    public void setDoctors(List<Doctor> doctors) {
        this.doctorList.clear();
        this.doctorList.addAll(doctors);
        notifyDataSetChanged();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDoctorName, textViewDoctorSpecialization;
        Button buttonBookAppointment;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDoctorName = itemView.findViewById(R.id.textViewDoctorName);
            textViewDoctorSpecialization = itemView.findViewById(R.id.textViewDoctorSpecialization);
            buttonBookAppointment = itemView.findViewById(R.id.buttonBookAppointment);
        }
    }
}