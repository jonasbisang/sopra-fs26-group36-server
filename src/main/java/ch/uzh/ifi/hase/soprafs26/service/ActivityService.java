package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;

@Service
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Autowired
    public ActivityService(@Qualifier("activityRepository") ActivityRepository activityRepository,
                           @Qualifier("userRepository") UserRepository userRepository, 
                           @Qualifier("groupRepository") GroupRepository groupRepository){
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public Activity createActivity(Activity newActivity, Long createdBy, Long targetGroupId) {
    
        validateActivityInputs(newActivity);

        parseTimeConditions(newActivity);
    
        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + createdBy));

        Group group = groupRepository.findById(targetGroupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        newActivity.setGroup(group);
        
        newActivity.setCreatedBy(creator);
        newActivity.setStatus(ActivityStatus.PENDING);
    
        return activityRepository.save(newActivity);
    }

    private void validateActivityInputs(Activity activity) {
        if (activity.getName() == null || activity.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity name cannot be empty!");
        }

        if (activity.getMinSize() > activity.getMaxSize()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum size cannot be greater than maximum size!");
            }
        if (activity.getMinSize() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum size must be at least 1!");
        }

        if (activity.getStartTime().isAfter(activity.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must be before end time!");
        }

        if (activity.getLocation() == null || activity.getLocation().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is required!");
        }
    }

    private void parseTimeConditions(Activity activity) {
        TimeWindow preference = activity.getTimePreference();
    
        if (preference == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time preference must be specified.");
        }
    
        if (preference != TimeWindow.CUSTOM) {
            activity.setStartTime(preference.getStartTime());
            activity.setEndTime(preference.getEndTime());
        } 
        else {
            if (activity.getStartTime() == null || activity.getEndTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Custom time slots require both a start and end time.");
            }
            
            if (!activity.getStartTime().isBefore(activity.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Start time must be before end time for custom slots.");
            }
        }
    }

    public List<Activity> getProposedActivitiesByGroupId(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found with ID: " + groupId));
    
        List<Activity> activities = activityRepository.findByGroupGroupId(groupId);
    
        return activities;
    }
}
