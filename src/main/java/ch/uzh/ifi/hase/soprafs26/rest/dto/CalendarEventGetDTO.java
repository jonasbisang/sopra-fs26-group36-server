package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;

public class CalendarEventGetDTO {
    private Long id;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String source;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}