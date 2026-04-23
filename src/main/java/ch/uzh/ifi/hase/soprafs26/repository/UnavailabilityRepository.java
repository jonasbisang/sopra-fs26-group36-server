package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import java.util.List;


public interface UnavailabilityRepository extends JpaRepository<Unavailability, Long> {
    List<Unavailability> findByUserId(Long userId);
    List<Unavailability> findByUserIn(List<User> users);
}
