package com.driveup.ui.ride;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    private Long id;
    private LocalDate date;
    private LocalTime startHour;
    private LocalTime endHour;
    private Double price;
}
