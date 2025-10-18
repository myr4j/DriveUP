package com.driveup.ui.importexport;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.driveup.databinding.FragmentImportExportBinding;

public class ImportExportFragment extends Fragment {

    private FragmentImportExportBinding binding;
    private ImportExportService importExportService;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentImportExportBinding.inflate(inflater, container, false);
        importExportService = new ImportExportServiceImpl(requireContext());
        setupFilePickerLauncher();
        setupButtonListeners();
        return binding.getRoot();
    }

    private void setupButtonListeners() {
        binding.buttonExport.setOnClickListener(v -> performExport());
        binding.buttonImport.setOnClickListener(v -> performImport());
    }
    
    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri selectedFileUri = data.getData();
                        importFromSelectedFile(selectedFileUri);
                    } else {
                        showStatus("❌ Aucun fichier sélectionné", false);
                    }
                } else {
                    showStatus("❌ Sélection de fichier annulée", false);
                }
            }
        );
    }


    private void performExport() {
        try {
            showStatus("🔄 Export en cours...", false);
            int exportedCount = importExportService.exportData();
            showStatus("✅ Export réussi!\n" + exportedCount + " courses exportées dans le dossier Téléchargements", true);
        } catch (Exception e) {
            Log.e("ImportExport", "Error during export", e);
            showStatus("❌ Erreur lors de l'export:\n" + e.getMessage(), false);
        } finally {
            setButtonsEnabled(true);
        }
    }

    private void performImport() {
        openFilePicker();
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            
            Intent chooser = Intent.createChooser(intent, "Sélectionner un fichier CSV");
            filePickerLauncher.launch(chooser);
        } catch (Exception e) {
            Log.e("ImportExport", "Error opening file picker", e);
            showStatus("❌ Erreur lors de l'ouverture du sélecteur de fichiers", false);
        }
    }

    private void importFromSelectedFile(Uri fileUri) {
        try {
            showStatus("🔄 Import en cours...", false);
            
            String fileName = getFileName(fileUri);
            ImportExportServiceImpl service = (ImportExportServiceImpl) importExportService;
            int importedCount = service.importDataFromUri(fileUri, fileName);
            
            showStatus("✅ Import réussi!\n" + importedCount + " courses ajoutées\nFichier: " + fileName, true);
            
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
            androidx.lifecycle.ViewModelProvider viewModelProvider = new androidx.lifecycle.ViewModelProvider(requireActivity(), new androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()));
            com.driveup.ui.ride.RideViewModel rideViewModel = viewModelProvider.get(com.driveup.ui.ride.RideViewModel.class);
            rideViewModel.refreshRides();
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