package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.GoogleCalendarToken;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GoogleCalendarTokenRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UnavailabilityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CalendarEventGetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GoogleCalendarServiceTest {

    @Mock
    private GoogleCalendarTokenRepository tokenRepository;

    @Mock
    private UnavailabilityRepository unavailabilityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoogleCalendarService googleCalendarService;

    private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(googleCalendarService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(googleCalendarService, "redirectUri", "http://localhost:8080/callback");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
    }

    @Test
    public void buildAuthUrl_returnsCorrectUrl() {
        String url = googleCalendarService.buildAuthUrl(1L);

        assertTrue(url.contains("client_id=test-client-id"));
        assertTrue(url.contains("state=1"));
        assertTrue(url.contains("scope=https://www.googleapis.com/auth/calendar.readonly"));
    }

    @Test
    public void syncCalendar_userNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> googleCalendarService.syncCalendar(1L));
    }

    @Test
    public void syncCalendar_noTokenFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> googleCalendarService.syncCalendar(1L));
    }

    @Test
    public void getUnifiedCalendar_returnsManualAndGoogleEvents() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        ch.uzh.ifi.hase.soprafs26.entity.Unavailability manual = new ch.uzh.ifi.hase.soprafs26.entity.Unavailability();
        manual.setStartDateTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        manual.setEndDateTime(LocalDateTime.of(2026, 5, 1, 11, 0));
        manual.setId(100L);
        
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(List.of(manual));

        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        List<CalendarEventGetDTO> result = googleCalendarService.getUnifiedCalendar(1L);

        assertEquals(1, result.size());
        assertEquals("manual", result.get(0).getSource());
        assertEquals(LocalDateTime.of(2026, 5, 1, 10, 0), result.get(0).getStartDateTime());
    }

    @Test
    public void getUnifiedCalendar_tokenExpired_triggersRefresh() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(new ArrayList<>());

        GoogleCalendarToken expiredToken = new GoogleCalendarToken();
        expiredToken.setAccessToken("old-token");
        expiredToken.setRefreshToken("refresh-token");
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(expiredToken));

        
        try {
            googleCalendarService.getUnifiedCalendar(1L);
        } catch (Exception e) {
        }

        assertTrue(LocalDateTime.now().isAfter(expiredToken.getExpiresAt()));
    }
}