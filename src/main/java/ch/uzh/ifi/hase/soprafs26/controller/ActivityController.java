package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ActivityService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityGetDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteDTO;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
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

    @PostMapping("/activities")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void createActivity(@RequestBody ActivityPostDTO activityPostDTO) {
        Activity activityInput = DTOMapper.INSTANCE.convertActivityPostDTOtoEntity(activityPostDTO);
        
        activityService.createActivity(activityInput, activityPostDTO.getCreatedBy(), activityPostDTO.getTargetGroupId());
    }

    @GetMapping("/groups/{groupId}/activities")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<ActivityGetDTO> getProposedActivities(@PathVariable Long groupId) {
        List<Activity> activities = activityService.getProposedActivitiesByGroupId(groupId);

        List<ActivityGetDTO> activityGetDTOs = new ArrayList<>();
        for (Activity activity : activities) {
            activityGetDTOs.add(DTOMapper.INSTANCE.convertEntityToActivityGetDTO(activity));
        }

        return activityGetDTOs;
    }

@PostMapping("/groups/{groupId}/activities/{activityId}/votes")
@ResponseStatus(HttpStatus.CREATED)
public void vote(@PathVariable Long groupId,
                 @PathVariable Long activityId,
                 @RequestBody ActivityVoteDTO dto,
                 @RequestHeader("Authorization") String token) {
    Long userId = Long.parseLong(token);
    activityService.vote(groupId, activityId, dto.isWantsToJoin(), userId);
}

}