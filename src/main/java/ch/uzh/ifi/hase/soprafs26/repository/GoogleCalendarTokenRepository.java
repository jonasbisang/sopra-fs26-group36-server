package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.GoogleCalendarToken;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, Long> {
    Optional<GoogleCalendarToken> findByUser(User user);
}
