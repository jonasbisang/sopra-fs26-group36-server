package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GroupGetDTO {
    private Long id;
    private String name;
    private int members;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMembers() { return members; }
    public void setMembers(int members) { this.members = members; }
}
