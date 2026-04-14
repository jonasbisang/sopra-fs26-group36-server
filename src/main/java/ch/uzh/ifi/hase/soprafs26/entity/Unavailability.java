package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import ch.uzh.ifi.hase.soprafs26.entity.User;


@Entity
@Table(name = "unavailabilities")
public class Unavailability implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    public User getUser() {
        return user; 
    }

    public void setUser(User user) {
        this.user = user; 
    }
}


