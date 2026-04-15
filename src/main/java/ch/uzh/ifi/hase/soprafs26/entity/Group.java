package ch.uzh.ifi.hase.soprafs26.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity 
@Table(name = "friend_groups")
public class Group implements Serializable { //maybe change Group to FriendGroup for no SQL confusion?
    private static final long serialVersionUID = 1L;

    @Id 
    @GeneratedValue 
    private Long groupId; //maybe change to Id?

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String joinPassword; //BCrypt hash in Service!

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId ) { this.groupId = groupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJoinPassword() { return joinPassword; }
    public void setJoinPassword(String joinPassword) {this.joinPassword = joinPassword; }
    public List<GroupMember> getMembers() {return members; }
    public void setMembers(List<GroupMember> members) {this.members = members; }
    
}