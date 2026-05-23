package model;

import java.time.LocalDate;

//Single crime record loaded from database

public class CrimeRecord extends DataRecord {
    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private String crimeType;
    private String description;
    private boolean arrested;
    private int hour;
    private double lat;
    private double lon;

    public CrimeRecord(int id, String district, LocalDate date,
                       String crimeType, String description,
                       boolean arrested, int hour) {
        super(id, district);
        this.date = date;
        this.crimeType = crimeType;
        this.description = description;
        this.arrested = arrested;
        this.hour = hour;
        this.lat = 0.0;
        this.lon = 0.0;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCrimeType() {
        return crimeType;
    }

    public String getDescription() {
        return description;
    }

    public boolean getArrested() {
        return arrested;
    }

    public int getHour() {
        return hour;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setCrimeType(String crimeType) {
        this.crimeType = crimeType;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public void setArrested(boolean arrested) {
        this.arrested = arrested;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public String getSummary() {
        return String.format("[%s] %s — District %s on %s",
                crimeType, description, district, date);
    }
}
