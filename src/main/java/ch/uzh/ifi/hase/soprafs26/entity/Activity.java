package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.constant.Weather;


import java.io.Serializable;
import java.time.LocalTime;

@Entity
@Table(name = "activities")
public class Activity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable=false)
	private String name;

    @Column
	private int minSize;

    @Column
	private int maxSize;

    @Column
	private int duration;
    
    @Enumerated(EnumType.STRING)
    @Column
	private TimeWindow timePreference;

    @Column(nullable=true)
    private LocalTime startTime;

    @Column(nullable=true)
    private LocalTime endTime;

    @Column
	private boolean isWeatherDependent;

    @Enumerated(EnumType.STRING)
    @Column(nullable=true)
    private Weather weather;

    @Column
	private String location;

    @Column
	private boolean isRecursive;

    @Column
	private ActivityStatus status;

    @ManyToOne
    @JoinColumn(name = "creator_id")
	private User createdBy;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TimeWindow getTimePreference() {
        return timePreference;
    }

    public void setTimePreference(TimeWindow timePreference) {
        this.timePreference = timePreference;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime){
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime){
        this.endTime = endTime;
    }

    public boolean isWeatherDependent() {
        return isWeatherDependent;
    }

    public void setWeatherDependent(boolean weatherDependent) {
        this.isWeatherDependent = weatherDependent;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isRecursive() {
        return isRecursive;
    }

    public void setRecursive(boolean recursive) {
        this.isRecursive = recursive;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
}
