package com.driveup.ui.stat;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayOfWeekStats {
    private Map<String, List<DayStats>> statsByMonth; // Format: "YYYY-MM" -> List of DayStats
    private Map<Integer, List<DayStats>> statsByYear; // Format: Year -> List of DayStats
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayStats {
        private String dayOfWeek; // "Lundi", "Mardi", etc.
        private int month; // 1-12
        private int year;
        private int rideCount;
        private double totalAmount;
    }
}

