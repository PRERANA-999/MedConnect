package com.example.medconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    // Optional: Add a listener interface if you want to click on user items (e.g., to view details)
    // private OnUserClickListener listener;

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    // Optional: Set a listener
    // public interface OnUserClickListener {
    //     void onUserClick(User user);
    // }
    // public void setOnUserClickListener(OnUserClickListener listener) {
    //     this.listener = listener;
    // }


    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.nameTextView.setText(user.getName());
        holder.emailTextView.setText(user.getEmail());
        holder.roleTextView.setText(user.getRole());

        // Handle specific fields based on role if needed
        if (user.getRole() != null && user.getRole().equalsIgnoreCase("doctor")) {
            holder.specializationTextView.setVisibility(View.VISIBLE);
            holder.qualificationTextView.setVisibility(View.VISIBLE);

            // CORRECTED: Use user.getQualification() for both, as per User.java mapping
            holder.specializationTextView.setText("Specialization: " + user.getQualification());
            holder.qualificationTextView.setText("Qualification: " + user.getQualification());
        } else {
            holder.specializationTextView.setVisibility(View.GONE);
            holder.qualificationTextView.setVisibility(View.GONE);
        }

        // Optional: Set click listener for the item
        // holder.itemView.setOnClickListener(v -> {
        //     if (listener != null) {
        //         listener.onUserClick(user);
        //     }
        // });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView emailTextView;
        TextView roleTextView;
        TextView specializationTextView; // For doctors
        TextView qualificationTextView;  // For doctors

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.userNameTextView);
            emailTextView = itemView.findViewById(R.id.userEmailTextView);
            roleTextView = itemView.findViewById(R.id.userRoleTextView);
            specializationTextView = itemView.findViewById(R.id.userSpecializationTextView);
            qualificationTextView = itemView.findViewById(R.id.userQualificationTextView);
        }
    }
}