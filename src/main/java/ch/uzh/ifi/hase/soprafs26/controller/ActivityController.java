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
                                                @RequestParam(required = false) String status) {
        List<Activity> activities;
        if ("SCHEDULED".equals(status)) {
            activities = activityService.getActivities(groupId);
        } else {
            activities = activityService.getProposedActivitiesByGroupId(groupId);
        }
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
            activityGetDTOs.add(dto);
        }
        return activityGetDTOs;
    }
    @PostMapping("/groups/{groupId}/activities/{activityId}/revive")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ActivityGetDTO reviveActivity(@PathVariable Long groupId,
                                      @PathVariable Long activityId,
                                      @RequestHeader("Authorization") String token) {
        Activity revived = activityService.reviveActivity(groupId, activityId, token);
        ActivityGetDTO dto = DTOMapper.INSTANCE.convertEntityToActivityGetDTO(revived);
        dto.setAcceptVotes(0); // new activity has no votes yet
        return dto;
    }

}