package com.driveup.ui.stat;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.driveup.DataBaseHelper;
import com.driveup.ui.ride.Ride;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatViewModel extends AndroidViewModel {

    private static final String TAG = "StatViewModel";
    
    // Constants pour les plages horaires
    private static final LocalTime SLOT_1_START = LocalTime.of(0, 0);
    private static final LocalTime SLOT_1_END = LocalTime.of(6, 0);
    private static final LocalTime SLOT_2_START = LocalTime.of(6, 0);
    private static final LocalTime SLOT_2_END = LocalTime.of(12, 0);
    private static final LocalTime SLOT_3_START = LocalTime.of(12, 0);
    private static final LocalTime SLOT_3_END = LocalTime.of(18, 0);
    private static final LocalTime SLOT_4_START = LocalTime.of(18, 0);
    private static final LocalTime SLOT_4_END = LocalTime.of(23, 59, 59);
    
    private static final String SLOT_1_LABEL = "00:00-06:00";
    private static final String SLOT_2_LABEL = "06:00-12:00";
    private static final String SLOT_3_LABEL = "12:00-18:00";
    private static final String SLOT_4_LABEL = "18:00-00:00";
    
    private static final String MONTH_KEY_SEPARATOR = "-";
    private static final String MONTH_KEY_FORMAT = "%02d";
    private static final Locale FRENCH_LOCALE = Locale.FRENCH;
    
    private final DataBaseHelper dbHelper;
    
    private final MutableLiveData<TotalStats> totalStatsLiveData = new MutableLiveData<>();
    private final MutableLiveData<DayOfWeekStats> dayOfWeekStatsLiveData = new MutableLiveData<>();
    private final MutableLiveData<TimeSlotStats> timeSlotStatsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public StatViewModel(@NonNull Application application) {
        super(application);
        dbHelper = DataBaseHelper.getInstance(application);
        loadStatistics();
    }

    public LiveData<TotalStats> getTotalStats() {
        return totalStatsLiveData;
    }

    public LiveData<DayOfWeekStats> getDayOfWeekStats() {
        return dayOfWeekStatsLiveData;
    }

    public LiveData<TimeSlotStats> getTimeSlotStats() {
        return timeSlotStatsLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void refreshStatistics() {
        loadStatistics();
    }

    private void loadStatistics() {
        isLoading.setValue(true);
        try {
            List<Ride> rides = dbHelper.getAllRides();
            
            totalStatsLiveData.setValue(calculateTotalStats(rides));
            dayOfWeekStatsLiveData.setValue(calculateDayOfWeekStats(rides));
            timeSlotStatsLiveData.setValue(calculateTimeSlotStats(rides));
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading statistics", e);
        } finally {
            isLoading.setValue(false);
        }
    }

    private TotalStats calculateTotalStats(List<Ride> rides) {
        int totalRides = rides.size();
        double totalAmount = rides.stream()
                .mapToDouble(Ride::getPrice)
                .sum();
        return new TotalStats(totalRides, totalAmount);
    }

    private DayOfWeekStats calculateDayOfWeekStats(List<Ride> rides) {
        Map<String, Map<DayOfWeek, List<Ride>>> ridesByMonthAndDay = groupRidesByMonthAndDay(rides);
        Map<Integer, Map<DayOfWeek, List<Ride>>> ridesByYearAndDay = groupRidesByYearAndDay(rides);

        Map<String, List<DayOfWeekStats.DayStats>> statsByMonth = convertToDayStatsByMonth(ridesByMonthAndDay);
        Map<Integer, List<DayOfWeekStats.DayStats>> statsByYear = convertToDayStatsByYear(ridesByYearAndDay);

        return new DayOfWeekStats(statsByMonth, statsByYear);
    }

    private Map<String, Map<DayOfWeek, List<Ride>>> groupRidesByMonthAndDay(List<Ride> rides) {
        Map<String, Map<DayOfWeek, List<Ride>>> result = new HashMap<>();
        for (Ride ride : rides) {
            LocalDate date = ride.getDate();
            String monthKey = createMonthKey(date.getYear(), date.getMonthValue());
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            
            result.computeIfAbsent(monthKey, k -> new HashMap<>())
                    .computeIfAbsent(dayOfWeek, k -> new ArrayList<>())
                    .add(ride);
        }
        return result;
    }

    private Map<Integer, Map<DayOfWeek, List<Ride>>> groupRidesByYearAndDay(List<Ride> rides) {
        Map<Integer, Map<DayOfWeek, List<Ride>>> result = new HashMap<>();
        for (Ride ride : rides) {
            LocalDate date = ride.getDate();
            int year = date.getYear();
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            
            result.computeIfAbsent(year, k -> new HashMap<>())
                    .computeIfAbsent(dayOfWeek, k -> new ArrayList<>())
                    .add(ride);
        }
        return result;
    }

    private Map<String, List<DayOfWeekStats.DayStats>> convertToDayStatsByMonth(
            Map<String, Map<DayOfWeek, List<Ride>>> ridesByMonthAndDay) {
        Map<String, List<DayOfWeekStats.DayStats>> statsByMonth = new LinkedHashMap<>();
        
        for (Map.Entry<String, Map<DayOfWeek, List<Ride>>> monthEntry : ridesByMonthAndDay.entrySet()) {
            String monthKey = monthEntry.getKey();
            int[] yearMonth = parseMonthKey(monthKey);
            
            List<DayOfWeekStats.DayStats> dayStatsList = convertDayRidesToStats(
                    monthEntry.getValue(), yearMonth[1], yearMonth[0]);
            sortStatsByAmountDescending(dayStatsList);
            
            statsByMonth.put(monthKey, dayStatsList);
        }
        return statsByMonth;
    }

    private Map<Integer, List<DayOfWeekStats.DayStats>> convertToDayStatsByYear(
            Map<Integer, Map<DayOfWeek, List<Ride>>> ridesByYearAndDay) {
        Map<Integer, List<DayOfWeekStats.DayStats>> statsByYear = new LinkedHashMap<>();
        
        for (Map.Entry<Integer, Map<DayOfWeek, List<Ride>>> yearEntry : ridesByYearAndDay.entrySet()) {
            int year = yearEntry.getKey();
            List<DayOfWeekStats.DayStats> dayStatsList = convertDayRidesToStats(
                    yearEntry.getValue(), 0, year);
            sortStatsByAmountDescending(dayStatsList);
            
            statsByYear.put(year, dayStatsList);
        }
        return statsByYear;
    }

    private List<DayOfWeekStats.DayStats> convertDayRidesToStats(
            Map<DayOfWeek, List<Ride>> dayRidesMap, int month, int year) {
        List<DayOfWeekStats.DayStats> dayStatsList = new ArrayList<>();
        
        for (Map.Entry<DayOfWeek, List<Ride>> dayEntry : dayRidesMap.entrySet()) {
            DayOfWeek dayOfWeek = dayEntry.getKey();
            List<Ride> dayRides = dayEntry.getValue();
            
            String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, FRENCH_LOCALE);
            int rideCount = dayRides.size();
            double totalAmount = calculateTotalAmount(dayRides);
            
            dayStatsList.add(new DayOfWeekStats.DayStats(dayName, month, year, rideCount, totalAmount));
        }
        return dayStatsList;
    }

    private TimeSlotStats calculateTimeSlotStats(List<Ride> rides) {
        Map<String, Map<String, List<Ride>>> ridesByMonthAndTimeSlot = groupRidesByMonthAndTimeSlot(rides);
        Map<Integer, Map<String, List<Ride>>> ridesByYearAndTimeSlot = groupRidesByYearAndTimeSlot(rides);

        Map<String, List<TimeSlotStats.TimeSlotData>> statsByMonth = 
                convertToTimeSlotStatsByMonth(ridesByMonthAndTimeSlot);
        Map<Integer, List<TimeSlotStats.TimeSlotData>> statsByYear = 
                convertToTimeSlotStatsByYear(ridesByYearAndTimeSlot);

        return new TimeSlotStats(statsByMonth, statsByYear);
    }

    private Map<String, Map<String, List<Ride>>> groupRidesByMonthAndTimeSlot(List<Ride> rides) {
        Map<String, Map<String, List<Ride>>> result = new HashMap<>();
        for (Ride ride : rides) {
            String timeSlot = getTimeSlotByDuration(ride.getStartHour(), ride.getEndHour());
            LocalDate date = ride.getDate();
            String monthKey = createMonthKey(date.getYear(), date.getMonthValue());
            
            result.computeIfAbsent(monthKey, k -> new HashMap<>())
                    .computeIfAbsent(timeSlot, k -> new ArrayList<>())
                    .add(ride);
        }
        return result;
    }

    private Map<Integer, Map<String, List<Ride>>> groupRidesByYearAndTimeSlot(List<Ride> rides) {
        Map<Integer, Map<String, List<Ride>>> result = new HashMap<>();
        for (Ride ride : rides) {
            String timeSlot = getTimeSlotByDuration(ride.getStartHour(), ride.getEndHour());
            int year = ride.getDate().getYear();
            
            result.computeIfAbsent(year, k -> new HashMap<>())
                    .computeIfAbsent(timeSlot, k -> new ArrayList<>())
                    .add(ride);
        }
        return result;
    }

    private Map<String, List<TimeSlotStats.TimeSlotData>> convertToTimeSlotStatsByMonth(
            Map<String, Map<String, List<Ride>>> ridesByMonthAndTimeSlot) {
        Map<String, List<TimeSlotStats.TimeSlotData>> statsByMonth = new LinkedHashMap<>();
        
        for (Map.Entry<String, Map<String, List<Ride>>> monthEntry : ridesByMonthAndTimeSlot.entrySet()) {
            String monthKey = monthEntry.getKey();
            int[] yearMonth = parseMonthKey(monthKey);
            
            List<TimeSlotStats.TimeSlotData> timeSlotDataList = convertTimeSlotRidesToStats(
                    monthEntry.getValue(), yearMonth[1], yearMonth[0]);
            sortTimeSlotStatsByAmountDescending(timeSlotDataList);
            
            statsByMonth.put(monthKey, timeSlotDataList);
        }
        return statsByMonth;
    }

    private Map<Integer, List<TimeSlotStats.TimeSlotData>> convertToTimeSlotStatsByYear(
            Map<Integer, Map<String, List<Ride>>> ridesByYearAndTimeSlot) {
        Map<Integer, List<TimeSlotStats.TimeSlotData>> statsByYear = new LinkedHashMap<>();
        
        for (Map.Entry<Integer, Map<String, List<Ride>>> yearEntry : ridesByYearAndTimeSlot.entrySet()) {
            int year = yearEntry.getKey();
            List<TimeSlotStats.TimeSlotData> timeSlotDataList = convertTimeSlotRidesToStats(
                    yearEntry.getValue(), 0, year);
            sortTimeSlotStatsByAmountDescending(timeSlotDataList);
            
            statsByYear.put(year, timeSlotDataList);
        }
        return statsByYear;
    }

    private List<TimeSlotStats.TimeSlotData> convertTimeSlotRidesToStats(
            Map<String, List<Ride>> timeSlotRidesMap, int month, int year) {
        List<TimeSlotStats.TimeSlotData> timeSlotDataList = new ArrayList<>();
        
        for (Map.Entry<String, List<Ride>> timeSlotEntry : timeSlotRidesMap.entrySet()) {
            String timeSlot = timeSlotEntry.getKey();
            List<Ride> slotRides = timeSlotEntry.getValue();
            
            int rideCount = slotRides.size();
            double totalAmount = calculateTotalAmount(slotRides);
            
            timeSlotDataList.add(new TimeSlotStats.TimeSlotData(timeSlot, month, year, rideCount, totalAmount));
        }
        return timeSlotDataList;
    }

    /**
     * Détermine la plage horaire d'une course en calculant le temps passé dans chaque plage
     * et en choisissant celle où la course passe le plus de temps.
     */
    private String getTimeSlotByDuration(LocalTime startTime, LocalTime endTime) {
        boolean crossesMidnight = endTime.isBefore(startTime) || endTime.equals(startTime);
        
        long[] durations = calculateDurationsInAllSlots(startTime, endTime, crossesMidnight);
        
        return findSlotWithMaxDuration(durations);
    }
    
    private long[] calculateDurationsInAllSlots(LocalTime startTime, LocalTime endTime, boolean crossesMidnight) {
        long[] durations = new long[4];
        
        if (crossesMidnight) {
            calculateDurationsCrossingMidnight(startTime, endTime, durations);
        } else {
            calculateDurationsSameDay(startTime, endTime, durations);
        }
        
        return durations;
    }
    
    private void calculateDurationsSameDay(LocalTime startTime, LocalTime endTime, long[] durations) {
        durations[0] = calculateDurationInSlot(startTime, endTime, SLOT_1_START, SLOT_1_END);
        durations[1] = calculateDurationInSlot(startTime, endTime, SLOT_2_START, SLOT_2_END);
        durations[2] = calculateDurationInSlot(startTime, endTime, SLOT_3_START, SLOT_3_END);
        durations[3] = calculateDurationInSlot(startTime, endTime, SLOT_4_START, SLOT_4_END);
    }
    
    private void calculateDurationsCrossingMidnight(LocalTime startTime, LocalTime endTime, long[] durations) {
        LocalTime endOfDay = LocalTime.of(23, 59, 59);
        LocalTime startOfDay = LocalTime.of(0, 0);
        
        // Durée dans la journée du début
        durations[0] = calculateDurationInSlot(startTime, endOfDay, SLOT_1_START, SLOT_1_END);
        durations[1] = calculateDurationInSlot(startTime, endOfDay, SLOT_2_START, SLOT_2_END);
        durations[2] = calculateDurationInSlot(startTime, endOfDay, SLOT_3_START, SLOT_3_END);
        durations[3] = calculateDurationInSlot(startTime, endOfDay, SLOT_4_START, SLOT_4_END);
        
        // Durée dans la journée suivante
        durations[0] += calculateDurationInSlot(startOfDay, endTime, SLOT_1_START, SLOT_1_END);
        durations[1] += calculateDurationInSlot(startOfDay, endTime, SLOT_2_START, SLOT_2_END);
        durations[2] += calculateDurationInSlot(startOfDay, endTime, SLOT_3_START, SLOT_3_END);
    }
    
    private String findSlotWithMaxDuration(long[] durations) {
        long maxDuration = Math.max(Math.max(durations[0], durations[1]), 
                                   Math.max(durations[2], durations[3]));
        
        if (maxDuration == durations[0]) {
            return SLOT_1_LABEL;
        } else if (maxDuration == durations[1]) {
            return SLOT_2_LABEL;
        } else if (maxDuration == durations[2]) {
            return SLOT_3_LABEL;
        } else {
            return SLOT_4_LABEL;
        }
    }
    
    /**
     * Calcule le temps (en minutes) qu'une course passe dans une plage horaire donnée.
     */
    private long calculateDurationInSlot(LocalTime startTime, LocalTime endTime, 
                                        LocalTime slotStart, LocalTime slotEnd) {
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            return 0;
        }
        
        LocalTime effectiveStart = clampTimeToSlot(startTime, slotStart, slotEnd);
        LocalTime effectiveEnd = clampTimeToSlot(endTime, slotStart, slotEnd);
        
        if (!effectiveEnd.isAfter(effectiveStart)) {
            return 0;
        }
        
        return (effectiveEnd.toSecondOfDay() - effectiveStart.toSecondOfDay()) / 60;
    }
    
    private LocalTime clampTimeToSlot(LocalTime time, LocalTime slotStart, LocalTime slotEnd) {
        if (time.isBefore(slotStart)) {
            return slotStart;
        }
        return time.isAfter(slotEnd) ? slotEnd : time;
    }

    // Helper methods
    private String createMonthKey(int year, int month) {
        return year + MONTH_KEY_SEPARATOR + String.format(MONTH_KEY_FORMAT, month);
    }

    private int[] parseMonthKey(String monthKey) {
        String[] parts = monthKey.split(MONTH_KEY_SEPARATOR);
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private double calculateTotalAmount(List<Ride> rides) {
        return rides.stream().mapToDouble(Ride::getPrice).sum();
    }

    private void sortStatsByAmountDescending(List<DayOfWeekStats.DayStats> statsList) {
        statsList.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
    }

    private void sortTimeSlotStatsByAmountDescending(List<TimeSlotStats.TimeSlotData> statsList) {
        statsList.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
    }
}
