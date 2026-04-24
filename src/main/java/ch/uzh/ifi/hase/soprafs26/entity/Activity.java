package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.constant.RainPreference;

import java.io.Serializable;
import java.time.LocalDateTime;
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
	private boolean weatherDependent;

    @Column
	private int maxTemp;

    @Column
	private int minTemp;

    @Enumerated(EnumType.STRING)
    @Column(nullable=true)
    private RainPreference rainPreference;

    @Column
	private String location;

    @Column
	private boolean isRecursive;

    @Column
	private ActivityStatus status;

    @Column
    private LocalDateTime scheduledTime;

    @ManyToOne
    @JoinColumn(name = "creator_id")
	private User createdBy;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

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
        return weatherDependent;
    }

    public void setWeatherDependent(boolean weatherDependent) {
        this.weatherDependent = weatherDependent;
    }

    public int getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(int maxTemp) {
        this.maxTemp = maxTemp;
    }

    public int getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(int minTemp) {
        this.minTemp = minTemp;
    }

    public RainPreference getRainPreference() {
        return rainPreference;
    }

    public void setRainPreference(RainPreference rainPreference) {
        this.rainPreference = rainPreference;
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

    public Group getGroup() { 
        return group; }

    public void setGroup(Group group) {
         this.group = group; }

    public LocalDateTime getScheduledTime() {
         return scheduledTime; }


    public void setScheduledTime(LocalDateTime scheduledTime) {
         this.scheduledTime = scheduledTime; }
}
