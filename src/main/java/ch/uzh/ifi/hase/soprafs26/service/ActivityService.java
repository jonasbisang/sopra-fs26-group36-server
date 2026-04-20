package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Autowired
    public ActivityService(@Qualifier("activityRepository") ActivityRepository activityRepository,
                           @Qualifier("userRepository") UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    public Activity createActivity(Activity newActivity, Long createdBy) {
    
        validateActivityInputs(newActivity);
    
        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + createdBy));
        
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
}
