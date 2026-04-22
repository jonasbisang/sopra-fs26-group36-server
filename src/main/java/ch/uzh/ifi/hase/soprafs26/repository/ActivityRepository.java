package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("activityRepository")
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByGroupGroupId(Long groupId);
    
    List<Activity> findByCreatedBy(User createdBy);

    List<Activity> findByStatus(ActivityStatus status);

    List<Activity> findByTimePreference(TimeWindow timePreference);
    
}