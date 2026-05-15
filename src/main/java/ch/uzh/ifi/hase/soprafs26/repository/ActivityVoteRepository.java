package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityVoteRepository extends JpaRepository<ActivityVote, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);
    long countByActivityIdAndWantsToJoinTrue(Long activityId);
    
    List<ActivityVote> findByActivityId(Long activityId);
    void deleteByActivityId(Long activityId);

    Optional<ActivityVote> findByActivityIdAndUserId(Long activityId, Long userId);

    @Query("SELECT v.activity FROM ActivityVote v WHERE v.user.id = :userId AND v.activity.group.groupId = :groupId AND v.wantsToJoin = false AND v.activity.status = ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus.PENDING")
    List<Activity> findRejectedActivitiesByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Query("SELECT v.activity FROM ActivityVote v WHERE v.user.id = :userId AND v.activity.group.groupId = :groupId AND v.wantsToJoin = true AND v.activity.status = ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus.PENDING")
    List<Activity> findAcceptedActivitiesByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);
}