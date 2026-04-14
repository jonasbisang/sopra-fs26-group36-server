package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.service.GoogleCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                                            @RequestParam String state) { // ✅ state instead of userId
        Long userId = Long.parseLong(state);
        googleCalendarService.handleCallback(code, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/users/{userId}/calendar/sync")
    public ResponseEntity<Void> syncCalendar(@PathVariable Long userId,
                                              @RequestHeader("Authorization") String token) {
        googleCalendarService.syncCalendar(userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}