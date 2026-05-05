package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ActivityVoteDTO;
import ch.uzh.ifi.hase.soprafs26.service.ActivityService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest; 
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
public class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private ActivityVoteRepository activityVoteRepository;


    @Test
    public void getProposedActivities_returnsListWithVotes() throws Exception {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setName("Coding");
        activity.setStatus(ActivityStatus.PENDING);

        User participant = new User();
        participant.setUsername("ciaran");

        ActivityVote vote = new ActivityVote();
        vote.setWantsToJoin(true);
        vote.setUser(participant);

        given(activityService.getProposedActivitiesByGroupId(1L)).willReturn(Collections.singletonList(activity));
        given(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(10L)).willReturn(1L);
        given(activityVoteRepository.findByActivityId(10L)).willReturn(Collections.singletonList(vote));

        mockMvc.perform(get("/groups/1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Coding")))
                .andExpect(jsonPath("$[0].acceptVotes", is(1)))
                .andExpect(jsonPath("$[0].participantUsernames[0]", is("ciaran")));
    }

    @Test
    public void vote_validInput_success() throws Exception {
        ActivityVoteDTO voteDTO = new ActivityVoteDTO();
        voteDTO.setUserId(1L);
        voteDTO.setWantsToJoin(true);

        MockHttpServletRequestBuilder postRequest = post("/groups/1/activities/10/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(voteDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated());

        Mockito.verify(activityService, Mockito.times(1))
                .vote(1L, 10L, true, 1L);
    }

    @Test
    public void getGroupCalendar_returnsScheduledActivities() throws Exception {
        Activity scheduledActivity = new Activity();
        scheduledActivity.setId(5L);
        scheduledActivity.setName("Party");
        scheduledActivity.setStatus(ActivityStatus.SCHEDULED);

        given(activityService.getGroupCalendar(Mockito.eq(1L), Mockito.anyString()))
                .willReturn(Collections.singletonList(scheduledActivity));
        given(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(5L)).willReturn(10L);

        mockMvc.perform(get("/groups/1/calendar")
                .header("Authorization", "token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Party")))
                .andExpect(jsonPath("$[0].acceptVotes", is(10)));
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}