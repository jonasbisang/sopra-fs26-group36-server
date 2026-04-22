package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;


@Entity
@Table(name = "activity_vote",
    uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "user_id"})
)
public class ActivityVote implements Serializable {

    @Id
	@GeneratedValue
	private Long id;


    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean wantsToJoin;


    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }


    public Activity getActivity() { return activity; }

    public void setActivity(Activity activity) {
         this.activity = activity; }


    public User getUser() { return user; }

    public void setUser(User user) {
         this.user = user; }


    public boolean isWantsToJoin() { return wantsToJoin; }

    public void setWantsToJoin(boolean wantsToJoin) { 
        this.wantsToJoin = wantsToJoin; }

}
