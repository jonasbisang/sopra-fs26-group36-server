package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteDTO;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
import java.util.List;

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
public List<Activity> getActivities(@PathVariable Long groupId,
                                     @RequestHeader("Authorization") String token) {
    return activityService.getActivities(groupId);
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