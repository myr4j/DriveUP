package com.driveup.ui.importexport;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.driveup.DataBaseHelper;
import com.driveup.R;
import com.driveup.ui.ride.Ride;
import com.driveup.databinding.FragmentImportExportBinding;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ImportExportFragment extends Fragment {

    private static final int STORAGE_PERMISSION_CODE = 1002;
    
    private FragmentImportExportBinding binding;
    private ImportExportService importExportService;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    static {
        System.out.println("ImportExportFragment class loaded");
    }

    public ImportExportFragment() {
        super();
        System.out.println("ImportExportFragment constructor called");
        android.util.Log.d("ImportExport", "ImportExportFragment constructor called");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("ImportExport", "onCreateView called - START");
        android.util.Log.d("ImportExport", "onCreateView called - START");
        System.out.println("onCreateView called - START");

        try {
            binding = FragmentImportExportBinding.inflate(inflater, container, false);
            Log.d("ImportExport", "Binding inflated successfully");
        } catch (Exception e) {
            Log.e("ImportExport", "Error inflating binding", e);
            android.util.Log.e("ImportExport", "Error inflating binding", e);
            return null;
        }

        importExportService = new ImportExportServiceImpl(requireContext());
        Log.d("ImportExport", "Service created successfully");

        setupFilePickerLauncher();
        Log.d("ImportExport", "File picker launcher setup complete");
        
        setupButtonListeners();
        Log.d("ImportExport", "onCreateView completed successfully");
        
        return binding.getRoot();
    }

    private void setupButtonListeners() {
        Log.d("ImportExport", "setupButtonListeners called - START");
        android.util.Log.d("ImportExport", "setupButtonListeners called - START");
        System.out.println("setupButtonListeners called - START");
        
        try {
            Log.d("ImportExport", "Setting up export button listener");
            binding.buttonExport.setOnClickListener(v -> {
                Log.d("ImportExport", "Export button clicked");
                if (checkStoragePermission()) {
                    performExport();
                }
            });

            Log.d("ImportExport", "Setting up import button listener");
            binding.buttonImport.setOnClickListener(v -> {
                Log.d("ImportExport", "Import button clicked - START");
                android.util.Log.d("ImportExport", "Import button clicked - START");
                System.out.println("Import button clicked - START");
                
                try {
                    Log.d("ImportExport", "About to check storage permission");
                    boolean hasPermission = checkStoragePermission();
                    Log.d("ImportExport", "Storage permission result: " + hasPermission);
                    
                    if (hasPermission) {
                        Log.d("ImportExport", "Calling performImport");
                        performImport();
                    } else {
                        Log.d("ImportExport", "Permission denied, not calling performImport");
                    }
                } catch (Exception e) {
                    Log.e("ImportExport", "Error in button click", e);
                    android.util.Log.e("ImportExport", "Error in button click", e);
                }
                
                Log.d("ImportExport", "Import button clicked - END");
            });
            
        } catch (Exception e) {
            Log.e("ImportExport", "Error setting up button listeners", e);
            android.util.Log.e("ImportExport", "Error setting up button listeners", e);
        }
        
        Log.d("ImportExport", "Button listeners setup complete");
    }
    
    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d("ImportExport", "File picker result received - resultCode: " + result.getResultCode());
                android.util.Log.d("ImportExport", "File picker result received - resultCode: " + result.getResultCode());
                System.out.println("File picker result received - resultCode: " + result.getResultCode());
                
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri selectedFileUri = data.getData();
                        Log.d("ImportExport", "Selected file URI: " + selectedFileUri.toString());
                        android.util.Log.d("ImportExport", "Selected file URI: " + selectedFileUri.toString());
                        System.out.println("Selected file URI: " + selectedFileUri.toString());
                        importFromSelectedFile(selectedFileUri);
                    } else {
                        Log.w("ImportExport", "No file selected");
                        showStatus("❌ Aucun fichier sélectionné", false);
                    }
                } else {
                    Log.w("ImportExport", "File selection cancelled");
                    showStatus("❌ Sélection de fichier annulée", false);
                }
            }
        );
    }

    private boolean checkStoragePermission() {
        // Pour l'import via sélecteur de fichiers, pas besoin de permissions de stockage
        // sur Android 11+ (API 30+)
        Log.d("ImportExport", "Skipping storage permission check for file picker");
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Plus besoin de gérer les permissions de stockage pour le sélecteur de fichiers
        Log.d("ImportExport", "onRequestPermissionsResult called but not needed for file picker");
    }

    private void performExport() {
        try {
            showStatus("🔄 Export en cours...", false);
            
            Log.d("ImportExport", "Calling export service");
            int exportedCount = importExportService.exportData();
            Log.d("ImportExport", "Export completed, count: " + exportedCount);
            
            showStatus("✅ Export réussi!\n" + exportedCount + " courses exportées dans le dossier Téléchargements", true);
        } catch (Exception e) {
            Log.e("ImportExport", "Error during export", e);
            showStatus("❌ Erreur lors de l'export:\n" + e.getMessage(), false);
        } finally {
            setButtonsEnabled(true);
        }
    }

    private void performImport() {
        Log.d("ImportExport", "performImport called - START");
        android.util.Log.d("ImportExport", "performImport called - START");
        System.out.println("performImport called - START");
        
        try {
            Log.d("ImportExport", "Starting file picker for import");
            Log.d("ImportExport", "Calling openFilePicker directly");
            openFilePicker();
        } catch (Exception e) {
            Log.e("ImportExport", "Error in performImport", e);
            android.util.Log.e("ImportExport", "Error in performImport", e);
        }
    }

    private void openFilePicker() {
        Log.d("ImportExport", "openFilePicker called - START");
        android.util.Log.d("ImportExport", "openFilePicker called - START");
        System.out.println("openFilePicker called - START");
        
        try {
            Log.d("ImportExport", "Creating intent...");
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            // Accepter tous les types de fichiers pour plus de flexibilité
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            
            Log.d("ImportExport", "Creating chooser...");
            Intent chooser = Intent.createChooser(intent, "Sélectionner un fichier CSV");
            
            Log.d("ImportExport", "About to launch file picker...");
            filePickerLauncher.launch(chooser);
            Log.d("ImportExport", "File picker launch completed");
        } catch (Exception e) {
            Log.e("ImportExport", "Error opening file picker", e);
            android.util.Log.e("ImportExport", "Error opening file picker", e);
            showStatus("❌ Erreur lors de l'ouverture du sélecteur de fichiers", false);
        }
    }

    private void importFromSelectedFile(Uri fileUri) {
        try {
            showStatus("🔄 Import en cours...", false);
            
            String fileName = getFileName(fileUri);
            Log.d("ImportExport", "Selected file name: " + fileName);
            
            ImportExportServiceImpl service = (ImportExportServiceImpl) importExportService;
            int importedCount = service.importDataFromUri(fileUri, fileName);
            Log.d("ImportExport", "Import completed, count: " + importedCount);
            
            showStatus("✅ Import réussi!\n" + importedCount + " courses ajoutées\nFichier: " + fileName, true);
            
            // Notifier les autres fragments que les données ont été mises à jour
            notifyDataChanged();
        } catch (Exception e) {
            Log.e("ImportExport", "Error during import", e);
            showStatus("❌ Erreur lors de l'import:\n" + e.getMessage(), false);
        } finally {
            setButtonsEnabled(true);
        }
    }
    
    private void notifyDataChanged() {
        try {
            // Utiliser le même scope d'activité pour accéder au même ViewModel
            androidx.lifecycle.ViewModelProvider viewModelProvider = new androidx.lifecycle.ViewModelProvider(requireActivity(), new androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()));
            com.driveup.ui.ride.RideViewModel rideViewModel = viewModelProvider.get(com.driveup.ui.ride.RideViewModel.class);
            rideViewModel.refreshRides();
            Log.d("ImportExport", "Notified RideViewModel to refresh data");
        } catch (Exception e) {
            Log.e("ImportExport", "Error notifying data change", e);
        }
    }

    private String getFileName(Uri uri) {
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex);
                    }
                }
            }
        }
        String result = uri.getPath();
        int cut = result.lastIndexOf('/');
        if (cut != -1) {
            result = result.substring(cut + 1);
        }
        return result;
    }

    private void showStatus(String message, boolean isSuccess) {
        binding.textStatus.setText(message);
        binding.textStatus.setTextColor(isSuccess ? 
            getResources().getColor(android.R.color.holo_green_dark) : 
            getResources().getColor(android.R.color.holo_red_dark));
        binding.textStatus.setVisibility(View.VISIBLE);
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.buttonImport.setEnabled(enabled);
        binding.buttonExport.setAlpha(enabled ? 1.0f : 0.6f);
        binding.buttonImport.setAlpha(enabled ? 1.0f : 0.6f);
    }
}