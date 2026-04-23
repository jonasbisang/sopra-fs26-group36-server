package ch.uzh.ifi.hase.soprafs26.rest.dto;
public class ActivityVoteDTO {
    private boolean wantsToJoin;
    private Long userId;


    public boolean isWantsToJoin() { return wantsToJoin; }
    public void setWantsToJoin(boolean wantsToJoin) { this.wantsToJoin = wantsToJoin; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
