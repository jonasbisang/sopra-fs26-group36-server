package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.constant.Weather;

import java.time.LocalTime;

public class ActivityPostDTO {

    private String name;

    private Integer minSize;

    private Integer maxSize;

    private Integer duration;

    private TimeWindow timePreference;

    private LocalTime startTime;

    private LocalTime endTime;

    private Boolean isWeatherDependent;

    private Weather weather;

    private String location;

    private Boolean isRecursive;

    private Long createdBy;


    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getMinSize() { return minSize; }
    public void setMinSize(int minSize) { this.minSize = minSize; }

    public Integer getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public Integer getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public TimeWindow getTimePreference() { return timePreference; }
    public void setTimePreference(TimeWindow timePreference) { this.timePreference = timePreference; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Boolean getIsWeatherDependent() { return isWeatherDependent; }
    public void setIsWeatherDependent(Boolean weatherDependent) { this.isWeatherDependent = isWeatherDependent; }

    public Weather getWeather() { return weather; }
    public void setWeather(Weather weather) { this.weather = weather; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Boolean getIsRecursive() { return isRecursive; }
    public void setIsRecursive(Boolean recursive) { this.isRecursive = isRecursive; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}