package com.example.medconnect;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    public interface OnTimeSlotClickListener {
        void onTimeSlotClick(Map<String, String> timeSlotMap);
    }

    private List<Map<String, String>> timeSlots;
    private OnTimeSlotClickListener listener;

    public TimeSlotAdapter(List<Map<String, String>> timeSlots, OnTimeSlotClickListener listener) {
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    // Method to update the list for BookAppointmentActivity
    public void updateTimeSlots(List<Map<String, String>> newTimeSlots) {
        this.timeSlots.clear();
        this.timeSlots.addAll(newTimeSlots);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        Map<String, String> slotMap = timeSlots.get(position);
        holder.bind(slotMap, listener);
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    public static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView typeTextView;
        TextView statusTextView;

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }

        public void bind(final Map<String, String> slotMap, final OnTimeSlotClickListener listener) {
            timeTextView.setText(slotMap.get("time"));
            typeTextView.setText(slotMap.get("type"));

            boolean isBooked = Boolean.parseBoolean(Objects.requireNonNull(slotMap.get("isBooked")));

            if (isBooked) {
                itemView.setBackgroundColor(Color.parseColor("#CCCCCC")); // Grey background for booked
                timeTextView.setTextColor(Color.parseColor("#808080"));
                typeTextView.setTextColor(Color.parseColor("#808080"));
                statusTextView.setText("Booked");
                statusTextView.setTextColor(Color.parseColor("#F44336")); // Red for "Booked"
                itemView.setClickable(false);
                itemView.setFocusable(false);
            } else {
                itemView.setBackgroundColor(Color.WHITE); // White background for available
                timeTextView.setTextColor(Color.BLACK);
                typeTextView.setTextColor(Color.parseColor("#3F51B5")); // Blue for type text
                statusTextView.setText("Available");
                statusTextView.setTextColor(Color.parseColor("#4CAF50")); // Green for "Available"
                itemView.setClickable(true);
                itemView.setFocusable(true);
                itemView.setOnClickListener(v -> listener.onTimeSlotClick(slotMap));
            }
        }
    }
}