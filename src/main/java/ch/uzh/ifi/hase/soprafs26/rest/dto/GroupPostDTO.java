package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GroupPostDTO {
    private String name;
    private String joinPassword;

    public String getName() { return name; }
    public void setName(String name) {this.name = name; }
    
    public String getJoinPassword() {return joinPassword; }
    public void setJoinPassword( String joinPassword) {this.joinPassword = joinPassword;}
} 
