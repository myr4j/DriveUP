package com.driveup.ui.importexport;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRideSummary {
    private LocalDate date;
    private LocalTime firstRideStart;
    private LocalTime lastRideEnd;
    private int rideCount;
    private double totalPrice;
}