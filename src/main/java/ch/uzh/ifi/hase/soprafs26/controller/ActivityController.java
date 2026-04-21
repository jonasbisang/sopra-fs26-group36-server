package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.ActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


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
        
        activityService.createActivity(activityInput, activityPostDTO.getCreatedBy());
    }
}