package com.example.medconnect; // <<< IMPORTANT: ENSURE THIS PACKAGE NAME MATCHES YOUR PROJECT'S PACKAGE NAME

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedSlotsAdapter extends RecyclerView.Adapter<SelectedSlotsAdapter.SelectedSlotViewHolder> {

    private List<String> selectedSlots;
    private OnSlotActionListener listener;

    // Interface for click events on the remove button within each slot item
    public interface OnSlotActionListener {
        void onRemoveClick(String slot);
    }

    // Constructor for the adapter
    public SelectedSlotsAdapter(List<String> selectedSlots, OnSlotActionListener listener) {
        this.selectedSlots = selectedSlots;
        this.listener = listener;
    }

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
    @NonNull
    @Override
    public SelectedSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single item (defined in item_selected_slot.xml)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_slot, parent, false);
        return new SelectedSlotViewHolder(view);
    }

    // Called by RecyclerView to display the data at the specified position.
    // This method updates the contents of the ViewHolder to reflect the item at the given position.
    @Override
    public void onBindViewHolder(@NonNull SelectedSlotViewHolder holder, int position) {
        String slotTime = selectedSlots.get(position);
        holder.textViewSelectedSlotTime.setText(slotTime);

        // Set a click listener on the remove icon
        holder.imageViewRemoveSlot.setOnClickListener(v -> {
            if (listener != null) {
                // Pass the time slot string back to the activity/fragment
                // so it knows which slot was requested for removal
                listener.onRemoveClick(slotTime);
            }
        });
    }

    // Returns the total number of items in the data set held by the adapter.
    @Override
    public int getItemCount() {
        return selectedSlots.size();
    }

    // ViewHolder class: Holds references to the views for each item in the RecyclerView.
    // This helps avoid repeatedly calling findViewById() which is expensive.
    public static class SelectedSlotViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSelectedSlotTime;
        ImageView imageViewRemoveSlot;

        public SelectedSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the views from item_selected_slot.xml using their IDs
            textViewSelectedSlotTime = itemView.findViewById(R.id.textViewSelectedSlotTime);
            imageViewRemoveSlot = itemView.findViewById(R.id.imageViewRemoveSlot);
        }
    }
}