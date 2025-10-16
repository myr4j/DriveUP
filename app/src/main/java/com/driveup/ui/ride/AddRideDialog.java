package com.driveup.ui.ride;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.driveup.databinding.DialogAddRideBinding;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AddRideDialog extends DialogFragment {

    private DialogAddRideBinding binding;
    private OnRideAddedListener listener;
    private LocalDate selectedDate = LocalDate.now();
    private LocalTime selectedStartTime = LocalTime.of(9, 30);
    private LocalTime selectedEndTime = LocalTime.of(10, 45);
    
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public interface OnRideAddedListener {
        void onRideAdded(Ride ride);
    }

    public static AddRideDialog newInstance(OnRideAddedListener listener) {
        AddRideDialog dialog = new AddRideDialog();
        dialog.listener = listener;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogAddRideBinding.inflate(LayoutInflater.from(getContext()));
        
        setupClickListeners();
        updateDisplayedValues();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(binding.getRoot());
        
        return builder.create();
    }

    private void setupClickListeners() {
        binding.editRideDate.setOnClickListener(v -> showDatePicker());

        binding.editRideStartTime.setOnClickListener(v -> showTimePicker(true));

        binding.editRideEndTime.setOnClickListener(v -> showTimePicker(false));

        binding.buttonCancel.setOnClickListener(v -> dismiss());

        binding.buttonSave.setOnClickListener(v -> saveRide());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateDisplayedValues();
                },
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth()
        );
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isStartTime) {
        LocalTime currentTime = isStartTime ? selectedStartTime : selectedEndTime;
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    LocalTime selectedTime = LocalTime.of(hourOfDay, minute);
                    if (isStartTime) {
                        selectedStartTime = selectedTime;
                    } else {
                        selectedEndTime = selectedTime;
                    }
                    updateDisplayedValues();
                },
                currentTime.getHour(),
                currentTime.getMinute(),
                true
        );
        timePickerDialog.show();
    }

    private void updateDisplayedValues() {
        binding.editRideDate.setText(selectedDate.format(dateFormatter));
        binding.editRideStartTime.setText(selectedStartTime.format(timeFormatter));
        binding.editRideEndTime.setText(selectedEndTime.format(timeFormatter));
    }

    private void saveRide() {
        try {
            // Validate input
            String priceText = binding.editRidePrice.getText().toString().trim();
            if (priceText.isEmpty()) {
                Toast.makeText(getContext(), "Veuillez saisir un prix", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceText);
            if (price < 0) {
                Toast.makeText(getContext(), "Le prix doit être positif", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedEndTime.isBefore(selectedStartTime) || selectedEndTime.equals(selectedStartTime)) {
                Toast.makeText(getContext(), "L'heure de fin doit être après l'heure de début", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create ride
            Ride ride = new Ride();
            ride.setDate(selectedDate);
            ride.setStartHour(selectedStartTime);
            ride.setEndHour(selectedEndTime);
            ride.setPrice(price);

            if (listener != null) {
                listener.onRideAdded(ride);
            }

            dismiss();
            Toast.makeText(getContext(), "Course ajoutée avec succès!", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Prix invalide", Toast.LENGTH_SHORT).show();
        }
    }
}