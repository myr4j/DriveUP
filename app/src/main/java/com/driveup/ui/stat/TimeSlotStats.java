package com.driveup.ui.stat;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotStats {
    private Map<String, List<TimeSlotData>> statsByMonth; // Format: "YYYY-MM" -> List of TimeSlotData
    private Map<Integer, List<TimeSlotData>> statsByYear; // Format: Year -> List of TimeSlotData
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotData {
        private String timeSlot; // "06:00-12:00", "12:00-18:00", etc.
        private int month; // 1-12
        private int year;
        private int rideCount;
        private double totalAmount;
    }
}

