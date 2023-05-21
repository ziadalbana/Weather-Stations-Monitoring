package com.example.Central.station;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WeatherData {
    @JsonProperty("humidity")
    private int humidity;
    @JsonProperty("temperature")
    private int temperature;
    @JsonProperty("wind_speed")
    private int windSpeed;

    // Getters and setters

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(int windSpeed) {
        this.windSpeed = windSpeed;
    }
}
