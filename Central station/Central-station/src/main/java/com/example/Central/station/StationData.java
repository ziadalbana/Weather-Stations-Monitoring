package com.example.Central.station;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StationData {
    @JsonProperty("station_id")
    private int stationId;
    @JsonProperty("s_no")
    private int sNo;
    @JsonProperty("battery_status")
    private String batteryStatus;
    @JsonProperty("status_timestamp")
    private long statusTimestamp;

    private WeatherData weather;

    // Getters and setters

    public int getStationId() {
        return stationId;
    }

    public void setStationId(int stationId) {
        this.stationId = stationId;
    }

    public int getSNo() {
        return sNo;
    }

    public void setSNo(int sNo) {
        this.sNo = sNo;
    }

    public String getBatteryStatus() {
        return batteryStatus;
    }

    public void setBatteryStatus(String batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public long getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(long statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public WeatherData getWeather() {
        return weather;
    }

    public void setWeather(WeatherData weather) {
        this.weather = weather;
    }
}

