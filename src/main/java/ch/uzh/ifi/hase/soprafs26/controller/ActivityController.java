package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ActivityService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityGetDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteDTO;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;

import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList; 


@RestController
public class ActivityController {

private final ActivityService activityService;
private final ActivityVoteRepository activityVoteRepository;
private final UserRepository userRepository;

public ActivityController(ActivityService activityService,
                          ActivityVoteRepository activityVoteRepository,
                        UserRepository userRepository) {
    this.activityService = activityService;
    this.activityVoteRepository = activityVoteRepository;
    this.userRepository = userRepository;
}
    @PostMapping("/groups/{groupId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public void createActivity(@PathVariable Long groupId,
                           @RequestBody ActivityPostDTO activityPostDTO,
                           @RequestHeader("Authorization") String token) {
    Activity activityInput = DTOMapper.INSTANCE.convertActivityPostDTOtoEntity(activityPostDTO);
    activityService.createActivity(activityInput, activityPostDTO.getCreatedBy(), groupId);
}

    @GetMapping("/groups/{groupId}/activities")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<ActivityGetDTO> getProposedActivities(
            @PathVariable Long groupId,
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String token) { // <-- 1. Add the token header

        // 2. Get the user making the request (using your UserService)
        User user = userRepository.findByToken(token);
        List<Activity> activities;

        // 3. Route the request based on the status string
        if ("SCHEDULED".equalsIgnoreCase(status)) {
            activities = activityService.getActivities(groupId);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            activities = activityService.getRejectedActivities(groupId, user.getId()); 
        } else if ("ACCEPTED".equalsIgnoreCase(status)) {
            // New route for "Awaiting Members"
            activities = activityService.getAcceptedActivities(groupId, user.getId());
        } else {
            // "PENDING" now strictly means "Upcoming Ideas I haven't voted on"
            activities = activityService.getUnvotedPendingActivities(groupId, user.getId());
        }

        List<ActivityGetDTO> activityGetDTOs = new ArrayList<>();
        for (Activity activity : activities) {
            ActivityGetDTO dto = DTOMapper.INSTANCE.convertEntityToActivityGetDTO(activity);
            
            // 1. Calculate and set acceptVotes
            long votes = activityVoteRepository.countByActivityIdAndWantsToJoinTrue(activity.getId());
            dto.setAcceptVotes((int) votes);
            
            // 2. Calculate and set the participantUsernames 
            List<String> participantUsernames = activityVoteRepository.findByActivityId(activity.getId())
                .stream()
                .filter(vote -> vote.isWantsToJoin()) 
                .map(vote -> vote.getUser().getUsername()) 
                .collect(java.util.stream.Collectors.toList());
                
            dto.setParticipantUsernames(participantUsernames);
            
            activityGetDTOs.add(dto);
        }
        return activityGetDTOs;
    }

    @PostMapping("/groups/{groupId}/activities/{activityId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public void vote(@PathVariable Long groupId,
                 @PathVariable Long activityId,
                 @RequestBody ActivityVoteDTO dto) {
        activityService.vote(groupId, activityId, dto.isWantsToJoin(), dto.getUserId());
    }

    @GetMapping("/groups/{groupId}/calendar")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<ActivityGetDTO> getGroupCalendar(@PathVariable Long groupId,
                                              @RequestHeader("Authorization") String token) {
        List<Activity> activities = activityService.getGroupCalendar(groupId, token);
        List<ActivityGetDTO> activityGetDTOs = new ArrayList<>();
        for (Activity activity : activities) {
            ActivityGetDTO dto = DTOMapper.INSTANCE.convertEntityToActivityGetDTO(activity);
            long votes = activityVoteRepository.countByActivityIdAndWantsToJoinTrue(activity.getId());
            dto.setAcceptVotes((int) votes);
            List<String> usernames = activityVoteRepository.findByActivityId(activity.getId())
            .stream()
            .filter(ActivityVote::isWantsToJoin)
            .map(v -> v.getUser().getUsername())
            .collect(Collectors.toList());
            dto.setParticipantUsernames(usernames);
            activityGetDTOs.add(dto);
        }
        return activityGetDTOs;
    }

    @PostMapping("/activities/{activityId}/revive")
    @ResponseStatus(HttpStatus.OK)
    public void reviveActivity(@PathVariable Long activityId,
                               @RequestHeader("Authorization") String token) {
        activityService.reviveActivity(activityId);
    }

}