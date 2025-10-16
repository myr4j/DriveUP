package com.driveup.ui.importexport;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.driveup.DataBaseHelper;
import com.driveup.databinding.FragmentImportExportBinding;
import com.driveup.ui.ride.Ride;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ImportExportFragment extends Fragment {

    private FragmentImportExportBinding binding;
    private ImportExportService importExportService;
    private static final int STORAGE_PERMISSION_CODE = 100;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ImportExportViewModel notificationsViewModel =
                new ViewModelProvider(this).get(ImportExportViewModel.class);

        binding = FragmentImportExportBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        importExportService = new ImportExportServiceImpl(requireContext());

        setupButtonListeners();
        //addTestDataIfNeeded();

        return root;
    }

    private void setupButtonListeners() {
        binding.buttonExport.setOnClickListener(v -> {
            Log.d("ImportExport", "Export button clicked");
            if (checkStoragePermission()) {
                performExport();
            }
        });

        binding.buttonImport.setOnClickListener(v -> {
            Log.d("ImportExport", "Import button clicked");
            if (checkStoragePermission()) {
                performImport();
            }
        });
        
        Log.d("ImportExport", "Button listeners setup complete");
    }

    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            return true;
        }
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("ImportExport", "Requesting storage permission");
            ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                    STORAGE_PERMISSION_CODE);
            return false;
        }
        Log.d("ImportExport", "Storage permission granted");
        return true;
    }

    private void performExport() {
        Log.d("ImportExport", "Starting export process");
        try {
            setButtonsEnabled(false);
            showStatus("🔄 Export en cours...", false);
            
            Log.d("ImportExport", "Calling export service");
            int exportedCount = importExportService.exportData();
            Log.d("ImportExport", "Export completed, count: " + exportedCount);
            
            showStatus("✅ Export réussi!\n" + exportedCount + " courses exportées dans le dossier Téléchargements", true);
        } catch (Exception e) {
            Log.e("ImportExport", "Export error", e);
            showStatus("❌ Erreur lors de l'export:\n" + e.getMessage(), false);
        } finally {
            setButtonsEnabled(true);
        }
    }

    private void performImport() {
        Log.d("ImportExport", "Starting import process");
        try {
            setButtonsEnabled(false);
            showStatus("🔄 Import en cours...", false);
            
            Log.d("ImportExport", "Calling import service");
            int importedCount = importExportService.importData();
            Log.d("ImportExport", "Import completed, count: " + importedCount);
            
            showStatus("✅ Import réussi!\n" + importedCount + " courses importées\nDonnées sauvegardées", true);
        } catch (Exception e) {
            Log.e("ImportExport", "Import error", e);
            showStatus("❌ Erreur lors de l'import:\n" + e.getMessage(), false);
        } finally {
            setButtonsEnabled(true);
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        binding.textStatus.setText(message);
        binding.textStatus.setTextColor(isSuccess ? 
                getResources().getColor(android.R.color.holo_green_dark) : 
                getResources().getColor(android.R.color.holo_red_dark));
        binding.textStatus.setVisibility(View.VISIBLE);
    }
    
    private void setButtonsEnabled(boolean enabled) {
        binding.buttonExport.setEnabled(enabled);
        binding.buttonImport.setEnabled(enabled);
        binding.buttonExport.setAlpha(enabled ? 1.0f : 0.6f);
        binding.buttonImport.setAlpha(enabled ? 1.0f : 0.6f);
    }
    
    private void addTestDataIfNeeded() {
        try {
            DataBaseHelper dbHelper = DataBaseHelper.getInstance(requireContext());
            List<Ride> existingRides = dbHelper.getAllRides();
            
            if (existingRides.isEmpty()) {
                Log.d("ImportExport", "Adding test data");
                List<Ride> testRides = new ArrayList<>();
                
                Ride ride1 = new Ride();
                ride1.setDate(LocalDate.now());
                ride1.setStartHour(LocalTime.of(9, 30));
                ride1.setEndHour(LocalTime.of(10, 45));
                ride1.setPrice(25.50);
                testRides.add(ride1);
                
                Ride ride2 = new Ride();
                ride2.setDate(LocalDate.now().minusDays(1));
                ride2.setStartHour(LocalTime.of(14, 20));
                ride2.setEndHour(LocalTime.of(15, 30));
                ride2.setPrice(18.75);
                testRides.add(ride2);
                
                Ride ride3 = new Ride();
                ride3.setDate(LocalDate.now().minusDays(2));
                ride3.setStartHour(LocalTime.of(16, 0));
                ride3.setEndHour(LocalTime.of(17, 15));
                ride3.setPrice(32.00);
                testRides.add(ride3);
                
                dbHelper.insertRides(testRides);
                Log.d("ImportExport", "Test data added: " + testRides.size() + " rides");
            }
        } catch (Exception e) {
            Log.e("ImportExport", "Error adding test data", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}