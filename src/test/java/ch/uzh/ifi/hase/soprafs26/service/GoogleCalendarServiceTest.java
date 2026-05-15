package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.GoogleCalendarToken;
import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GoogleCalendarTokenRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UnavailabilityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CalendarEventGetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GoogleCalendarServiceTest {

    @Mock private GoogleCalendarTokenRepository tokenRepository;
    @Mock private UnavailabilityRepository unavailabilityRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GoogleCalendarService googleCalendarService;

    private User testUser;
    private GoogleCalendarToken validToken;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(googleCalendarService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(googleCalendarService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(googleCalendarService, "redirectUri", "http://localhost/callback");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        validToken = new GoogleCalendarToken();
        validToken.setUser(testUser);
        validToken.setAccessToken("access-token");
        validToken.setRefreshToken("refresh-token");
        validToken.setExpiresAt(LocalDateTime.now().plusHours(1));
    }


    @Test
    public void buildAuthUrl_containsClientId() {
        String url = googleCalendarService.buildAuthUrl(1L);
        assertTrue(url.contains("client_id=test-client-id"));
    }

    @Test
    public void buildAuthUrl_containsState() {
        String url = googleCalendarService.buildAuthUrl(42L);
        assertTrue(url.contains("state=42"));
    }

    @Test
    public void buildAuthUrl_containsRedirectUri() {
        String url = googleCalendarService.buildAuthUrl(1L);
        assertTrue(url.contains("redirect_uri="));
    }

    @Test
    public void buildAuthUrl_containsOfflineAccess() {
        String url = googleCalendarService.buildAuthUrl(1L);
        assertTrue(url.contains("access_type=offline"));
    }

    @Test
    public void buildAuthUrl_containsConsentPrompt() {
        String url = googleCalendarService.buildAuthUrl(1L);
        assertTrue(url.contains("prompt=consent"));
    }

    @Test
    public void buildAuthUrl_differentUserIds_differentUrls() {
        String url1 = googleCalendarService.buildAuthUrl(1L);
        String url2 = googleCalendarService.buildAuthUrl(2L);
        assertNotEquals(url1, url2);
    }


    @Test
    public void syncCalendar_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> googleCalendarService.syncCalendar(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void syncCalendar_noTokenConnected_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> googleCalendarService.syncCalendar(1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }


    @Test
    public void getUnifiedCalendar_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> googleCalendarService.getUnifiedCalendar(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getUnifiedCalendar_noEntries_returnsEmptyList() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        List<CalendarEventGetDTO> result = googleCalendarService.getUnifiedCalendar(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getUnifiedCalendar_withEntries_returnsMappedDTOs() {
        Unavailability u1 = new Unavailability();
        u1.setId(10L);
        u1.setStartDateTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        u1.setEndDateTime(LocalDateTime.of(2026, 6, 1, 17, 0));
        u1.setSource("manual");

        Unavailability u2 = new Unavailability();
        u2.setId(11L);
        u2.setStartDateTime(LocalDateTime.of(2026, 6, 2, 10, 0));
        u2.setEndDateTime(LocalDateTime.of(2026, 6, 2, 12, 0));
        u2.setSource("google");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(List.of(u1, u2));

        List<CalendarEventGetDTO> result = googleCalendarService.getUnifiedCalendar(1L);

        assertEquals(2, result.size());
    }

    @Test
    public void getUnifiedCalendar_resultIsSortedByStartDate() {
        Unavailability u1 = new Unavailability();
        u1.setId(10L);
        u1.setStartDateTime(LocalDateTime.of(2026, 6, 5, 9, 0));
        u1.setEndDateTime(LocalDateTime.of(2026, 6, 5, 10, 0));

        Unavailability u2 = new Unavailability();
        u2.setId(11L);
        u2.setStartDateTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        u2.setEndDateTime(LocalDateTime.of(2026, 6, 1, 10, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(List.of(u1, u2));

        List<CalendarEventGetDTO> result = googleCalendarService.getUnifiedCalendar(1L);

        assertTrue(result.get(0).getStartDateTime().isBefore(result.get(1).getStartDateTime()));
    }

    @Test
    public void getUnifiedCalendar_idsMappedCorrectly() {
        Unavailability u = new Unavailability();
        u.setId(42L);
        u.setStartDateTime(LocalDateTime.now());
        u.setEndDateTime(LocalDateTime.now().plusHours(1));
        u.setSource("manual");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(List.of(u));

        List<CalendarEventGetDTO> result = googleCalendarService.getUnifiedCalendar(1L);

        assertEquals(42L, result.get(0).getId());
    }

    @Test
    public void getUnifiedCalendar_sourcePreserved() {
        Unavailability u = new Unavailability();
        u.setId(1L);
        u.setStartDateTime(LocalDateTime.now());
        u.setEndDateTime(LocalDateTime.now().plusHours(1));
        u.setSource("google");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(List.of(u));

        List<CalendarEventGetDTO> result = googleCalendarService.getUnifiedCalendar(1L);

        assertEquals("google", result.get(0).getSource());
    }


    @Test
    public void createCalendarEvent_noTokenForUser_doesNothing() {
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
            googleCalendarService.createCalendarEvent(
                testUser, "Event", "Zurich",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1)
            )
        );
    }

    @Test
    public void createCalendarEvent_tokenExists_attemptsApiCall() {
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(validToken));

        assertDoesNotThrow(() ->
            googleCalendarService.createCalendarEvent(
                testUser, "Hike", "Alps",
                LocalDateTime.now(), LocalDateTime.now().plusHours(2)
            )
        );
    }


    @Test
    public void syncAllGoogleCalendars_noTokens_nothingHappens() {
        when(tokenRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> googleCalendarService.syncAllGoogleCalendars());
    }

    @Test
    public void syncAllGoogleCalendars_failingSync_continuesWithOthers() {
        User failUser = new User();
        failUser.setId(99L);

        GoogleCalendarToken failToken = new GoogleCalendarToken();
        failToken.setUser(failUser);

        when(tokenRepository.findAll()).thenReturn(List.of(failToken));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> googleCalendarService.syncAllGoogleCalendars());
    }
}