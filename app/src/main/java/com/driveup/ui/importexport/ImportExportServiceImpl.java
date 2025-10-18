package com.driveup.ui.importexport;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.driveup.DataBaseHelper;
import com.driveup.ui.ride.Ride;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImportExportServiceImpl implements ImportExportService {
    
    private static final String TAG = "ImportExportService";
    private static final String CSV_HEADER = "ID,Date,Heure Début,Heure Fin,Prix";
    private static final String DAILY_CSV_HEADER = "Date,Heure Début Première Course,Heure Fin Dernière Course,Nombre de Courses,Total (€)";
    
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
                throw new RuntimeException("Fichier CSV introuvable dans Téléchargements");
            }
            
            List<Ride> rides = parseCsvFile(csvFile);
            if (rides == null || rides.isEmpty()) {
                throw new RuntimeException("Aucune donnée valide trouvée dans le fichier CSV");
            }

            dbHelper.insertRides(rides);
            return rides.size();
        } catch (Exception e) {
            Log.e(TAG, "Error importing data", e);
            throw new RuntimeException("Erreur lors de l'import: " + e.getMessage());
        }
    }
    
    
    public int importDataFromUri(Uri fileUri, String fileName) {
        try {
            List<Ride> rides = parseCsvFromUri(fileUri);
            if (rides == null || rides.isEmpty()) {
                throw new RuntimeException("Aucune donnée valide trouvée dans le fichier CSV");
            }

            dbHelper.insertRides(rides);
            return rides.size();
        } catch (Exception e) {
            Log.e(TAG, "Error importing data from URI", e);
            throw new RuntimeException("Erreur lors de l'import: " + e.getMessage());
        }
    }
    
    @Override
    public int exportData() {
        try {
            List<Ride> rides = dbHelper.getAllRides();
            if (rides.isEmpty()) {
                throw new RuntimeException("Aucune course à exporter");
            }
            
            File csvFile = getCsvFile();
            writeCsvFile(csvFile, rides);
            return rides.size();
        } catch (Exception e) {
            Log.e(TAG, "Error exporting data", e);
            throw new RuntimeException("Erreur lors de l'export: " + e.getMessage());
        }
    }

    @Override
    public int exportDailyData() {
        try {
            List<Ride> rides = dbHelper.getAllRides();
            if (rides.isEmpty()) {
                throw new RuntimeException("Aucune course à exporter");
            }
            
            List<DailyRideSummary> dailySummaries = createDailySummaries(rides);
            File csvFile = getDailyCsvFile();
            writeDailyCsvFile(csvFile, dailySummaries);
            return dailySummaries.size();
        } catch (Exception e) {
            Log.e(TAG, "Error exporting daily data", e);
            throw new RuntimeException("Erreur lors de l'export journalier: " + e.getMessage());
        }
    }
    
    private File getCsvFile() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = now.format(formatter);
        String filename = "courses_brut_" + timestamp + ".csv";
        
        return new File(downloadsDir, filename);
    }

    private File getDailyCsvFile() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = now.format(formatter);
        String filename = "courses_journalier_" + timestamp + ".csv";
        
        return new File(downloadsDir, filename);
    }
    
    private List<Ride> parseCsvFile(File csvFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            return parseCsvFromReader(br);
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV file", e);
            return null;
        }
    }
    
    private List<Ride> parseCsvFromUri(Uri fileUri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            return parseCsvFromReader(br);
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV file from URI", e);
            return null;
        }
    }
    
    private List<Ride> parseCsvFromReader(BufferedReader br) throws IOException {
        List<Ride> rides = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        String line;
        boolean isFirstLine = true;
        
        while ((line = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // Skip header
            }
            
            String[] values = line.split(",");
            if (values.length == 5) {
                try {
                    Ride ride = new Ride();
                    ride.setDate(LocalDate.parse(values[1].trim(), dateFormatter));
                    ride.setStartHour(LocalTime.parse(values[2].trim(), timeFormatter));
                    ride.setEndHour(LocalTime.parse(values[3].trim(), timeFormatter));
                    ride.setPrice(Double.parseDouble(values[4].trim()));
                    rides.add(ride);
                } catch (DateTimeParseException | NumberFormatException e) {
                    Log.w(TAG, "Skipping invalid line: " + line, e);
                }
            } else if (values.length == 4) {
                // Support old format without ID for backward compatibility
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
        
        return rides;
    }
    
    private void writeCsvFile(File csvFile, List<Ride> rides) throws IOException {
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.append(CSV_HEADER).append("\n");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            for (Ride ride : rides) {
                writer.append(String.valueOf(ride.getId()))
                      .append(",")
                      .append(ride.getDate().format(dateFormatter))
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

    private List<DailyRideSummary> createDailySummaries(List<Ride> rides) {
        // Grouper les courses par date
        Map<LocalDate, List<Ride>> ridesByDate = rides.stream()
                .collect(Collectors.groupingBy(Ride::getDate));

        List<DailyRideSummary> summaries = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Ride>> entry : ridesByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Ride> dayRides = entry.getValue();

            // Trier les courses par heure de début
            dayRides.sort(Comparator.comparing(Ride::getStartHour));

            // Calculer les statistiques
            LocalTime firstRideStart = dayRides.get(0).getStartHour();
            LocalTime lastRideEnd = dayRides.get(dayRides.size() - 1).getEndHour();
            int rideCount = dayRides.size();
            double totalPrice = dayRides.stream().mapToDouble(Ride::getPrice).sum();

            summaries.add(new DailyRideSummary(date, firstRideStart, lastRideEnd, rideCount, totalPrice));
        }

        // Trier par date
        summaries.sort(Comparator.comparing(DailyRideSummary::getDate));
        return summaries;
    }

    private void writeDailyCsvFile(File csvFile, List<DailyRideSummary> summaries) throws IOException {
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.append(DAILY_CSV_HEADER).append("\n");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            for (DailyRideSummary summary : summaries) {
                writer.append(summary.getDate().format(dateFormatter))
                      .append(",")
                      .append(summary.getFirstRideStart().format(timeFormatter))
                      .append(",")
                      .append(summary.getLastRideEnd().format(timeFormatter))
                      .append(",")
                      .append(String.valueOf(summary.getRideCount()))
                      .append(",")
                      .append(String.format("%.2f", summary.getTotalPrice()))
                      .append("\n");
            }
        }
    }
}
