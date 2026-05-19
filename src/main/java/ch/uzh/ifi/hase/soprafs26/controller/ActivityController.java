package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ActivityService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityGetDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteDTO;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;

import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList; 


@RestController
public class ActivityController {

private final ActivityService activityService;
private final ActivityVoteRepository activityVoteRepository;
public ActivityController(ActivityService activityService,
                          ActivityVoteRepository activityVoteRepository) {
    this.activityService = activityService;
    this.activityVoteRepository = activityVoteRepository;
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
    public List<ActivityGetDTO> getProposedActivities(@PathVariable Long groupId,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Long userId) {
        List<Activity> activities;

        if (("ACCEPTED".equals(status) || "REJECTED".equals(status)) && userId != null) {
            boolean wantsToJoin = "ACCEPTED".equals(status);
            activities = activityService.getActivitiesByUserVote(groupId, userId, wantsToJoin);}
            
        else if ("SCHEDULED".equals(status)) {
            activities = activityService.getActivities(groupId);} 
            
        else if ("PAST".equals(status)) {
            activities = activityService.getActivitiesByStatus(groupId, ActivityStatus.PAST);}
            
        else if ("FAILED".equals(status)) {
            activities = activityService.getActivitiesByStatus(groupId, ActivityStatus.FAILED);}
            
        else {
            activities = activityService.getProposedActivitiesByGroupId(groupId, userId);}
            
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
            activityGetDTOs.add(dto);}
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