package com.driveup.ui.ride;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.driveup.DataBaseHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class RideViewModel extends AndroidViewModel {

    private static final String TAG = "RideViewModel";
    private DataBaseHelper dbHelper;
    private MutableLiveData<List<Ride>> ridesLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public RideViewModel(@NonNull Application application) {
        super(application);
        dbHelper = DataBaseHelper.getInstance(application);
        loadRides();
    }

    public LiveData<List<Ride>> getRides() {
        return ridesLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void loadRides() {
        isLoading.setValue(true);
        try {
            List<Ride> rides = dbHelper.getAllRides();
            ridesLiveData.setValue(rides);
        } catch (Exception e) {
            Log.e(TAG, "Error loading rides", e);
        } finally {
            isLoading.setValue(false);
        }
    }

    public void addRide(Ride ride) {
        try {
            long id = dbHelper.insertRide(ride);
            ride.setId(id);
            loadRides(); // Refresh the list
        } catch (Exception e) {
            Log.e(TAG, "Error adding ride", e);
        }
    }

    public void deleteRide(Ride ride) {
        try {
            boolean deleted = dbHelper.deleteRide(ride.getId());
            if (deleted) {
                loadRides(); // Refresh the list
            } else {
                Log.w(TAG, "Failed to delete ride: " + ride.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting ride", e);
        }
    }

    public void refreshRides() {
        loadRides();
    }

}