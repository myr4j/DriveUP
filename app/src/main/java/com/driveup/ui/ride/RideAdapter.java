package com.driveup.ui.ride;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.driveup.R;
import com.driveup.databinding.ItemRideCardBinding;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private List<Ride> rides;
    private OnRideClickListener listener;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public interface OnRideClickListener {
        void onDeleteRide(Ride ride);
    }

    public RideAdapter(List<Ride> rides, OnRideClickListener listener) {
        this.rides = rides;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRideCardBinding binding = ItemRideCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RideViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        Ride ride = rides.get(position);
        holder.bind(ride);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    public void updateRides(List<Ride> newRides) {
        this.rides = newRides;
        notifyDataSetChanged();
    }

    class RideViewHolder extends RecyclerView.ViewHolder {
        private ItemRideCardBinding binding;

        public RideViewHolder(@NonNull ItemRideCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Ride ride) {
            // Format date
            String dateText = ride.getDate().format(dateFormatter);
            binding.textRideDate.setText(dateText);

            // Format times
            String startTime = ride.getStartHour().format(timeFormatter);
            String endTime = ride.getEndHour().format(timeFormatter);
            binding.textRideStartTime.setText(startTime);
            binding.textRideEndTime.setText(endTime);

            // Format price
            String priceText = String.format("%.2f €", ride.getPrice());
            binding.textRidePrice.setText(priceText);

            // Calculate and format duration
            Duration duration = Duration.between(ride.getStartHour(), ride.getEndHour());
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            String durationText = String.format("Durée: %dh %02dmin", hours, minutes);
            binding.textRideDuration.setText(durationText);

            // Set delete button click listener
            binding.buttonDeleteRide.setOnClickListener(v -> {
                Log.d("RideAdapter", "Delete button clicked for ride: " + ride.getId());
                if (listener != null) {
                    listener.onDeleteRide(ride);
                } else {
                    Log.w("RideAdapter", "Listener is null!");
                }
            });
        }
    }
}