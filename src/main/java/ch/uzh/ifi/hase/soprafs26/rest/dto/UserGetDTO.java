package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.nio.charset.StandardCharsets;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

public class UserGetDTO {

	private Long id;
	private String name;
	private String username;
	private String token;
	private UserStatus status;
	private String email;
	private String bio;
// + getters and setters
	//email?


    public UserGetDTO() {
    }
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getToken() {
		return token;
	}
	
	public void setToken(String token){
		this.token = token;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

    public String getEmail() {
		 return email; }

    public void setEmail(String email) {
		 this.email = email; }

    public String getBio() {
		 return bio; }

    public void setBio(String bio) { 
		this.bio = bio; }
	
}
