package com.driveup.ui.importexport;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.driveup.DataBaseHelper;
import com.driveup.ui.ride.Ride;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportExportServiceImpl implements ImportExportService {
    
    private static final String TAG = "ImportExportService";
    private static final String CSV_FILENAME = "driveup_rides.csv";
    private static final String CSV_HEADER = "Date,Heure Début,Heure Fin,Prix";
    
    private Context context;
    private DataBaseHelper dbHelper;
    
    public ImportExportServiceImpl(Context context) {
        this.context = context;
        this.dbHelper = DataBaseHelper.getInstance(context);
    }
    
    @Override
    public int importData() {
        try {
            File csvFile = getCsvFile();
            if (!csvFile.exists()) {
                Log.e(TAG, "CSV file does not exist");
                throw new RuntimeException("Fichier CSV introuvable dans Téléchargements");
            }
            
            List<Ride> rides = parseCsvFile(csvFile);
            if (rides == null || rides.isEmpty()) {
                Log.i(TAG, "No valid rides found in CSV");
                throw new RuntimeException("Aucune donnée valide trouvée dans le fichier CSV");
            }

            dbHelper.insertRides(rides);
            Log.i(TAG, "Successfully imported " + rides.size() + " rides");
            return rides.size();
        } catch (Exception e) {
            Log.e(TAG, "Error importing data", e);
            throw new RuntimeException("Erreur lors de l'import: " + e.getMessage());
        }
    }
    
    @Override
    public int exportData() {
        try {
            List<Ride> rides = dbHelper.getAllRides();
            if (rides.isEmpty()) {
                Log.i(TAG, "No rides to export");
                throw new RuntimeException("Aucune course à exporter");
            }
            
            File csvFile = getCsvFile();
            writeCsvFile(csvFile, rides);
            Log.i(TAG, "Successfully exported " + rides.size() + " rides to " + csvFile.getAbsolutePath());
            return rides.size();
        } catch (Exception e) {
            Log.e(TAG, "Error exporting data", e);
            throw new RuntimeException("Erreur lors de l'export: " + e.getMessage());
        }
    }
    
    private File getCsvFile() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(downloadsDir, CSV_FILENAME);
    }
    
    private List<Ride> parseCsvFile(File csvFile) {
        List<Ride> rides = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }
                
                String[] values = line.split(",");
                if (values.length == 4) {
                    try {
                        Ride ride = new Ride();
                        ride.setDate(LocalDate.parse(values[0].trim(), dateFormatter));
                        ride.setStartHour(LocalTime.parse(values[1].trim(), timeFormatter));
                        ride.setEndHour(LocalTime.parse(values[2].trim(), timeFormatter));
                        ride.setPrice(Double.parseDouble(values[3].trim()));
                        rides.add(ride);
                    } catch (DateTimeParseException | NumberFormatException e) {
                        Log.w(TAG, "Skipping invalid line: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV file", e);
            return null;
        }
        
        return rides;
    }
    
    private void writeCsvFile(File csvFile, List<Ride> rides) throws IOException {
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.append(CSV_HEADER).append("\n");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            for (Ride ride : rides) {
                writer.append(ride.getDate().format(dateFormatter))
                      .append(",")
                      .append(ride.getStartHour().format(timeFormatter))
                      .append(",")
                      .append(ride.getEndHour().format(timeFormatter))
                      .append(",")
                      .append(String.valueOf(ride.getPrice()))
                      .append("\n");
            }
        }
    }
}
