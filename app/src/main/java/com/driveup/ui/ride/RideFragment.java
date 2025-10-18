package com.driveup.ui.ride;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.driveup.databinding.FragmentRideBinding;


public class RideFragment extends Fragment implements RideAdapter.OnRideClickListener, AddRideDialog.OnRideAddedListener {

    private static final String TAG = "RideFragment";
    private FragmentRideBinding binding;
    private RideViewModel rideViewModel;
    private RideAdapter rideAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Utiliser le scope de l'activité pour partager le ViewModel entre fragments
        rideViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(RideViewModel.class);

        binding = FragmentRideBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();

        return root;
    }
    

    private void setupRecyclerView() {
        rideAdapter = new RideAdapter(null, this);
        binding.recyclerRides.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerRides.setAdapter(rideAdapter);
    }

    private void setupClickListeners() {
        binding.buttonAddRide.setOnClickListener(v -> showAddRideDialog());
    }

    private void observeViewModel() {
        rideViewModel.getRides().observe(getViewLifecycleOwner(), rides -> {
            if (rides != null) {
                rideAdapter.updateRides(rides);
                updateEmptyState(rides.isEmpty());
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.textEmptyRides.setVisibility(View.VISIBLE);
            binding.recyclerRides.setVisibility(View.GONE);
        } else {
            binding.textEmptyRides.setVisibility(View.GONE);
            binding.recyclerRides.setVisibility(View.VISIBLE);
        }
    }

    private void showAddRideDialog() {
        AddRideDialog dialog = AddRideDialog.newInstance(this);
        dialog.show(getParentFragmentManager(), "AddRideDialog");
    }

    @Override
    public void onDeleteRide(Ride ride) {
        rideViewModel.deleteRide(ride);
    }

    @Override
    public void onRideAdded(Ride ride) {
        rideViewModel.addRide(ride);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}