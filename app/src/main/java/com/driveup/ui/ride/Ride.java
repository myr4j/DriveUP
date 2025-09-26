package com.driveup.ui.ride;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ride {

    Long id;
    LocalDate date;
    LocalTime startHour;
    LocalTime endHour;
    Double price;

}
