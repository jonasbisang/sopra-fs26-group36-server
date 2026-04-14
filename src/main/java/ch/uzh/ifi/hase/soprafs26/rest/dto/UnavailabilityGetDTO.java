package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;


public class UnavailabilityGetDTO {
    private Long id;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

    public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(LocalDateTime endDateTime) {
		this.endDateTime = endDateTime;
    }

    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) {
        this.id = id; 
    }
}