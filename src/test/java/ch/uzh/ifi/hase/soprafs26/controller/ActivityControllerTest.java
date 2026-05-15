package ch.uzh.ifi.hase.soprafs26.controller;
 
import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMemberRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.service.ActivityService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
 
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
 
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private ActivityVoteRepository activityVoteRepository;

    private String asJson(Object o) throws Exception {
        return new ObjectMapper().writeValueAsString(o);
    }


    @Test
    public void getActivities_scheduledStatus_callsGetActivities() throws Exception {
        Activity a = new Activity();
        a.setId(1L);
        a.setName("Chess");
        a.setStatus(ActivityStatus.SCHEDULED);

        given(activityService.getActivities(1L)).willReturn(List.of(a));
        given(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).willReturn(0L);
        given(activityVoteRepository.findByActivityId(1L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/groups/1/activities?status=SCHEDULED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name", is("Chess")));
    }

    @Test
    public void getActivities_pastStatus_callsGetActivitiesByStatus() throws Exception {
        Activity a = new Activity();
        a.setId(2L);
        a.setName("Old Run");
        a.setStatus(ActivityStatus.PAST);

        given(activityService.getActivitiesByStatus(1L, ActivityStatus.PAST)).willReturn(List.of(a));
        given(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(2L)).willReturn(0L);
        given(activityVoteRepository.findByActivityId(2L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/groups/1/activities?status=PAST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name", is("Old Run")));
    }

    @Test
    public void getActivities_failedStatus_callsGetActivitiesByStatus() throws Exception {
        Activity a = new Activity();
        a.setId(3L);
        a.setName("Failed Trip");
        a.setStatus(ActivityStatus.FAILED);

        given(activityService.getActivitiesByStatus(1L, ActivityStatus.FAILED)).willReturn(List.of(a));
        given(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(3L)).willReturn(0L);
        given(activityVoteRepository.findByActivityId(3L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/groups/1/activities?status=FAILED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name", is("Failed Trip")));
    }

    @Test
    public void getActivities_noStatus_callsGetProposed() throws Exception {
        Activity a = new Activity();
        a.setId(4L);
        a.setName("Pending Hike");
        a.setStatus(ActivityStatus.PENDING);

        given(activityService.getProposedActivitiesByGroupId(1L)).willReturn(List.of(a));
        given(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(4L)).willReturn(0L);
        given(activityVoteRepository.findByActivityId(4L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/groups/1/activities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name", is("Pending Hike")));
    }


    @Test
    public void reviveActivity_valid_returns200() throws Exception {
        doNothing().when(activityService).reviveActivity(1L);

        mockMvc.perform(post("/activities/1/revive")
                .header("Authorization", "token-123"))
            .andExpect(status().isOk());
    }

    @Test
    public void reviveActivity_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
            .when(activityService).reviveActivity(99L);

        mockMvc.perform(post("/activities/99/revive")
                .header("Authorization", "token-123"))
            .andExpect(status().isNotFound());
    }


    @Test
    public void vote_activityNotFound_returns404() throws Exception {
        ActivityVoteDTO voteDTO = new ActivityVoteDTO();
        voteDTO.setUserId(1L);
        voteDTO.setWantsToJoin(true);

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
            .when(activityService).vote(anyLong(), anyLong(), anyBoolean(), anyLong());

        mockMvc.perform(post("/groups/1/activities/999/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(voteDTO)))
            .andExpect(status().isNotFound());
    }

    @Test
    public void vote_alreadyVoted_returns409() throws Exception {
        ActivityVoteDTO voteDTO = new ActivityVoteDTO();
        voteDTO.setUserId(1L);
        voteDTO.setWantsToJoin(true);

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already voted"))
            .when(activityService).vote(anyLong(), anyLong(), anyBoolean(), anyLong());

        mockMvc.perform(post("/groups/1/activities/1/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(voteDTO)))
            .andExpect(status().isConflict());
    }


    @Test
    public void getGroupCalendar_notMember_returns403() throws Exception {
        given(activityService.getGroupCalendar(Mockito.eq(1L), Mockito.anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/groups/1/calendar")
                .header("Authorization", "not-a-member-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void getGroupCalendar_notLoggedIn_returns401() throws Exception {
        given(activityService.getGroupCalendar(Mockito.eq(1L), Mockito.anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(get("/groups/1/calendar")
                .header("Authorization", "invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGroupCalendar_scheduledWithParticipants_returnsParticipants() throws Exception {
        Activity a = new Activity();
        a.setId(7L);
        a.setName("Picnic");
        a.setStatus(ActivityStatus.SCHEDULED);
        a.setScheduledTime(LocalDateTime.of(2026, 7, 4, 14, 0));

        User participant = new User();
        participant.setUsername("dave");

        ActivityVote vote = new ActivityVote();
        vote.setWantsToJoin(true);
        vote.setUser(participant);

        given(activityService.getGroupCalendar(Mockito.eq(1L), Mockito.anyString()))
            .willReturn(List.of(a));
        given(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(7L)).willReturn(1L);
        given(activityVoteRepository.findByActivityId(7L)).willReturn(List.of(vote));

        mockMvc.perform(get("/groups/1/calendar")
                .header("Authorization", "token-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name", is("Picnic")))
            .andExpect(jsonPath("$[0].acceptVotes", is(1)))
            .andExpect(jsonPath("$[0].participantUsernames[0]", is("dave")));
    }
}