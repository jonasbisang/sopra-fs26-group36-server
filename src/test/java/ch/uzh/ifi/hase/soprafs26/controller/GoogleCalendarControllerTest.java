package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.CalendarEventGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.GoogleCalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoogleCalendarController.class)
public class GoogleCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleCalendarService googleCalendarService;


    @Test
    public void getAuthUrl_valid_returns200WithUrl() throws Exception {
        given(googleCalendarService.buildAuthUrl(1L))
            .willReturn("https://accounts.google.com/o/oauth2/auth?client_id=test&state=1");

        mockMvc.perform(get("/auth/google").param("userId", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("accounts.google.com")));
    }

    @Test
    public void getAuthUrl_differentUser_returnsCorrectUrl() throws Exception {
        given(googleCalendarService.buildAuthUrl(42L))
            .willReturn("https://accounts.google.com/o/oauth2/auth?client_id=test&state=42");

        mockMvc.perform(get("/auth/google").param("userId", "42"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("state=42")));
    }


    @Test
    public void handleCallback_valid_redirects302() throws Exception {
        doNothing().when(googleCalendarService).handleCallback(anyString(), anyLong());

        mockMvc.perform(get("/auth/google/callback")
                .param("code", "auth-code-xyz")
                .param("state", "1"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", containsString("/users/1/calendar")));
    }

    @Test
    public void handleCallback_locationHeaderContainsUserId() throws Exception {
        doNothing().when(googleCalendarService).handleCallback(anyString(), anyLong());

        mockMvc.perform(get("/auth/google/callback")
                .param("code", "some-code")
                .param("state", "99"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", containsString("/users/99/calendar")));
    }

    @Test
    public void handleCallback_callsHandleCallbackWithCorrectArgs() throws Exception {
        doNothing().when(googleCalendarService).handleCallback(eq("my-code"), eq(5L));

        mockMvc.perform(get("/auth/google/callback")
                .param("code", "my-code")
                .param("state", "5"))
            .andExpect(status().isFound());
    }


    @Test
    public void syncCalendar_valid_returns200() throws Exception {
        doNothing().when(googleCalendarService).syncCalendar(1L);

        mockMvc.perform(post("/users/1/calendar/sync")
                .header("Authorization", "alice-token"))
            .andExpect(status().isOk());
    }

    @Test
    public void syncCalendar_userNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
            .when(googleCalendarService).syncCalendar(99L);

        mockMvc.perform(post("/users/99/calendar/sync")
                .header("Authorization", "alice-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void syncCalendar_noGoogleConnected_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No Google Calendar connected"))
            .when(googleCalendarService).syncCalendar(1L);

        mockMvc.perform(post("/users/1/calendar/sync")
                .header("Authorization", "alice-token"))
            .andExpect(status().isNotFound());
    }


    @Test
    public void getUnifiedCalendar_valid_returns200WithEvents() throws Exception {
        CalendarEventGetDTO dto = new CalendarEventGetDTO();
        dto.setId(1L);
        dto.setStartDateTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        dto.setEndDateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        dto.setSource("google");

        given(googleCalendarService.getUnifiedCalendar(1L)).willReturn(List.of(dto));

        mockMvc.perform(get("/users/1/calendar")
                .header("Authorization", "alice-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(1)))
            .andExpect(jsonPath("$[0].source", is("google")));
    }

    @Test
    public void getUnifiedCalendar_noEvents_returnsEmptyArray() throws Exception {
        given(googleCalendarService.getUnifiedCalendar(1L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/users/1/calendar")
                .header("Authorization", "alice-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getUnifiedCalendar_userNotFound_returns404() throws Exception {
        given(googleCalendarService.getUnifiedCalendar(99L))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/users/99/calendar")
                .header("Authorization", "alice-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getUnifiedCalendar_multipleEvents_allReturned() throws Exception {
        CalendarEventGetDTO e1 = new CalendarEventGetDTO();
        e1.setId(1L);
        e1.setStartDateTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        e1.setEndDateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        e1.setSource("manual");

        CalendarEventGetDTO e2 = new CalendarEventGetDTO();
        e2.setId(2L);
        e2.setStartDateTime(LocalDateTime.of(2026, 6, 2, 14, 0));
        e2.setEndDateTime(LocalDateTime.of(2026, 6, 2, 15, 0));
        e2.setSource("google");

        given(googleCalendarService.getUnifiedCalendar(1L)).willReturn(List.of(e1, e2));

        mockMvc.perform(get("/users/1/calendar")
                .header("Authorization", "alice-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].source", is("manual")))
            .andExpect(jsonPath("$[1].source", is("google")));
    }
}