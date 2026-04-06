package ch.uzh.ifi.hase.soprafs26.entity;
import ch.uzh.ifi.hase.soprafs26.constant.RoleType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "group_members")
public class GroupMember implements Serializable{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false)
    private RoleType role;

    @Column(nullable = false)
    private LocalDate joinDate;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) {this.memberId = memberId; }
    public User getUser() {return user; }
    public void setUser(User user) {this.user = user; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) {this.group = group; }
    public RoleType getRole() { return role; }
    public void setRole(RoleType role) {this.role = role; }
    public LocalDate getJoinDate() {return joinDate; }
    public void setJoinDate(LocalDate joinDate) {this.joinDate = joinDate; }
}
