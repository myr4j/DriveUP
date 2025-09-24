package com.driveup.ui.ride;

import java.time.LocalDate;
import java.time.LocalTime;

public class Ride {
    LocalDate date;
    LocalTime startHour;
    LocalTime endHour;
    Double price;

    public Ride(){}
    public Ride(LocalDate date, LocalTime startHour, LocalTime endHour, Double price) {
        this.date = date;
        this.startHour = startHour;
        this.endHour = endHour;
        this.price = price;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartHour() {
        return startHour;
    }

    public LocalTime getEndHour() {
        return endHour;
    }

    public Double getPrice() {
        return price;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setStartHour(LocalTime startHour) {
        this.startHour = startHour;
    }

    public void setEndHour(LocalTime endHour) {
        this.endHour = endHour;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
