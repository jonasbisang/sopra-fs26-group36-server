package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.constant.Weather;
import java.time.LocalTime;

public class ActivityGetDTO {

    private Long id;
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
    private ActivityStatus status;
    private Long creatorId;
    private Long groupId;  

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getMinSize() { return minSize; }
    public void setMinSize(Integer minSize) { this.minSize = minSize; }

    public Integer getMaxSize() { return maxSize; }
    public void setMaxSize(Integer maxSize) { this.maxSize = maxSize; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public TimeWindow getTimePreference() { return timePreference; }
    public void setTimePreference(TimeWindow timePreference) { this.timePreference = timePreference; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Boolean getIsWeatherDependent() { return isWeatherDependent; }
    public void setIsWeatherDependent(Boolean isWeatherDependent) { this.isWeatherDependent = isWeatherDependent; }

    public Weather getWeather() { return weather; }
    public void setWeather(Weather weather) { this.weather = weather; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Boolean getIsRecursive() { return isRecursive; }
    public void setIsRecursive(Boolean isRecursive) { this.isRecursive = isRecursive; }

    public ActivityStatus getStatus() { return status; }
    public void setStatus(ActivityStatus status) { this.status = status; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
}