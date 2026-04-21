package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface UnavailabilityRepository extends JpaRepository<Unavailability, Long> {
    List<Unavailability> findByUserId(Long userId);
}
