package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ActivityService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityGetDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList; 


@RestController
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

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
}