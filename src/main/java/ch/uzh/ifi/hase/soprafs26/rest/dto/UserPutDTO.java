package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class UserPutDTO {
    private String oldPassword;
    private String newPassword;
    private String newUsername;
    private String newBio;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getNewUsername() { return newUsername; }
    public void setNewUsername(String newUsername) { this.newUsername = newUsername; }
    public String getNewBio() {return newBio; }
    public void setNewBio(String newBio) {this.newBio = newBio; }
}