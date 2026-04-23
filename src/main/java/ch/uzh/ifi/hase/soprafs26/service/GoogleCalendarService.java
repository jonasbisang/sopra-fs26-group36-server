package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.GoogleCalendarToken;
import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GoogleCalendarTokenRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UnavailabilityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpEntity;

import ch.uzh.ifi.hase.soprafs26.rest.dto.CalendarEventGetDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GoogleCalendarService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private final GoogleCalendarTokenRepository tokenRepository;
    private final UnavailabilityRepository unavailabilityRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleCalendarService(GoogleCalendarTokenRepository tokenRepository,
                                  UnavailabilityRepository unavailabilityRepository,
                                  UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.userRepository = userRepository;
    }

    public String buildAuthUrl(Long userId) {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/calendar.readonly" +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + userId;
}

    public void handleCallback(String code, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token", params, Map.class);

        Map body = response.getBody();

        GoogleCalendarToken token = tokenRepository.findByUser(user)
                .orElse(new GoogleCalendarToken());
        token.setUser(user);
        token.setAccessToken((String) body.get("access_token"));
        token.setRefreshToken((String) body.get("refresh_token"));
        token.setExpiresAt(LocalDateTime.now().plusSeconds((Integer) body.get("expires_in")));
        tokenRepository.save(token);
    }

public void syncCalendar(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    GoogleCalendarToken token = tokenRepository.findByUser(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No Google Calendar connected"));

    if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
        refreshAccessToken(token);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token.getAccessToken());
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response = restTemplate.exchange(
            "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
            "?singleEvents=true&orderBy=startTime" +
            "&timeMin=2020-01-01T00:00:00Z" +
            "&timeMax=2030-01-01T00:00:00Z",
            HttpMethod.GET, entity, Map.class);

    if (response.getBody() == null || response.getBody().get("items") == null) {
        return;
    }

    List<Map> events = (List<Map>) response.getBody().get("items");

    for (Map event : events) {
        Map start = (Map) event.get("start");
        Map end = (Map) event.get("end");
        if (start == null || end == null) continue;

        String startStr = (String) start.getOrDefault("dateTime", start.get("date"));
        String endStr = (String) end.getOrDefault("dateTime", end.get("date"));
        if (startStr == null || endStr == null) continue;

        LocalDateTime startDT = startStr.length() == 10
                ? LocalDate.parse(startStr).atStartOfDay()
                : LocalDateTime.parse(startStr.substring(0, 19));
        LocalDateTime endDT = endStr.length() == 10
                ? LocalDate.parse(endStr).atStartOfDay()
                : LocalDateTime.parse(endStr.substring(0, 19));

        Unavailability unavailability = new Unavailability();
        unavailability.setUser(user);
        unavailability.setStartDateTime(startDT);
        unavailability.setEndDateTime(endDT);
        unavailabilityRepository.save(unavailability);
    }
}
    private void refreshAccessToken(GoogleCalendarToken token) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", token.getRefreshToken());
        params.add("grant_type", "refresh_token");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token", params, Map.class);

        Map body = response.getBody();
        token.setAccessToken((String) body.get("access_token"));
        token.setExpiresAt(LocalDateTime.now().plusSeconds((Integer) body.get("expires_in")));
        tokenRepository.save(token);
    }

    public List<CalendarEventGetDTO> getUnifiedCalendar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    
        List<CalendarEventGetDTO> events = new ArrayList<>();
    
        List<Unavailability> dbEntries = unavailabilityRepository.findByUserId(userId);
        for (Unavailability u : dbEntries) {
            CalendarEventGetDTO dto = new CalendarEventGetDTO();
            dto.setId(u.getId());
            dto.setStartDateTime(u.getStartDateTime());
            dto.setEndDateTime(u.getEndDateTime());
            dto.setSource("manual");
            events.add(dto);
        }
    
        Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUser(user);
        if (tokenOpt.isPresent()) {
            GoogleCalendarToken token = tokenOpt.get();
    
            if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
                refreshAccessToken(token);
            }
    
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.getAccessToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
    
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
                    "?singleEvents=true&orderBy=startTime" +
                    "&timeMin=2020-01-01T00:00:00Z" +
                    "&timeMax=2030-01-01T00:00:00Z",
                    HttpMethod.GET, entity, Map.class);
    
            if (response.getBody() != null && response.getBody().get("items") != null) {
                List<Map> googleEvents = (List<Map>) response.getBody().get("items");
                for (Map event : googleEvents) {
                    Map start = (Map) event.get("start");
                    Map end = (Map) event.get("end");
                    if (start == null || end == null) continue;
    
                    String startStr = (String) start.getOrDefault("dateTime", start.get("date"));
                    String endStr = (String) end.getOrDefault("dateTime", end.get("date"));
                    if (startStr == null || endStr == null) continue;
    
                    LocalDateTime startDT = startStr.length() == 10
                            ? LocalDate.parse(startStr).atStartOfDay()
                            : LocalDateTime.parse(startStr.substring(0, 19));
                    LocalDateTime endDT = endStr.length() == 10
                            ? LocalDate.parse(endStr).atStartOfDay()
                            : LocalDateTime.parse(endStr.substring(0, 19));
    
                    CalendarEventGetDTO dto = new CalendarEventGetDTO();
                    dto.setStartDateTime(startDT);
                    dto.setEndDateTime(endDT);
                    dto.setSource("google");
                    events.add(dto);
                }
            }
        }
    
        events.sort(Comparator.comparing(CalendarEventGetDTO::getStartDateTime));
        return events;
    }
}