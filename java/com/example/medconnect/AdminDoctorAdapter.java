package com.example.medconnect; // Ensure this matches your package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Import your Doctor model here (ensure the package path is correct)
// Example: import com.example.medconnect.models.Doctor;
// Or if it's directly in 'com.example.medconnect', no extra import needed.
import com.example.medconnect.Doctor; // Assuming your Doctor.java is in the same package

public class AdminDoctorAdapter extends RecyclerView.Adapter<AdminDoctorAdapter.DoctorViewHolder> {

    private List<Doctor> doctorList;
    private OnDoctorClickListener listener;

    // Interface for click events on doctor items
    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor doctor);
    }

    public AdminDoctorAdapter(List<Doctor> doctorList, OnDoctorClickListener listener) {
        this.doctorList = doctorList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single doctor item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_doctor, parent, false); // This layout needs to be created next!
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        Doctor doctor = doctorList.get(position);

        // Bind doctor data to the TextViews
        holder.doctorNameTextView.setText(doctor.getUsername()); // Or doctor.getName() if you use 'name'
        holder.doctorSpecializationTextView.setText(doctor.getSpecialization());
        holder.doctorEmailTextView.setText(doctor.getEmail());

        // Set click listener for the entire item view
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onDoctorClick(doctor); // Pass the clicked doctor object
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
    }

    // ViewHolder class to hold references to the views in each item
    public static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView doctorNameTextView;
        TextView doctorSpecializationTextView;
        TextView doctorEmailTextView;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize TextViews from the item_admin_doctor layout
            doctorNameTextView = itemView.findViewById(R.id.textViewDoctorName);
            doctorSpecializationTextView = itemView.findViewById(R.id.textViewDoctorSpecialization);
            doctorEmailTextView = itemView.findViewById(R.id.textViewDoctorEmail);
        }
    }
}