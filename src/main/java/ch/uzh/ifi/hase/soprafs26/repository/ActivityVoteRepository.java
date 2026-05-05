package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityVoteRepository extends JpaRepository<ActivityVote, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);
    long countByActivityIdAndWantsToJoinTrue(Long activityId);
    
    List<ActivityVote> findByActivityId(Long activityId);
    void deleteByActivityId(Long activityId);
    
}