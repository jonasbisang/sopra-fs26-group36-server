package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.service.GoogleCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CalendarEventGetDTO;
import java.util.List;
import org.springframework.http.HttpHeaders;

@RestController
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    public GoogleCalendarController(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    @GetMapping("/auth/google")
    public ResponseEntity<String> getAuthUrl(@RequestParam Long userId) {
        return ResponseEntity.ok(googleCalendarService.buildAuthUrl(userId));
    }

    @GetMapping("/auth/google/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam String code,
                                            @RequestParam String state) {
        Long userId = Long.parseLong(state);
        googleCalendarService.handleCallback(code, userId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "https://sopra-fs26-group36-client.vercel.app/users/" + userId + "/calendar");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/users/{userId}/calendar/sync")
    public ResponseEntity<Void> syncCalendar(@PathVariable Long userId,
                                              @RequestHeader("Authorization") String token) {
        googleCalendarService.syncCalendar(userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/users/{userId}/calendar")
    public ResponseEntity<List<CalendarEventGetDTO>> getUnifiedCalendar(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(googleCalendarService.getUnifiedCalendar(userId));
}
}