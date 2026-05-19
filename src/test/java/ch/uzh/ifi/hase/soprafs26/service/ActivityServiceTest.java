package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.RainPreference;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ActivityServiceTest {

    @Mock private ActivityRepository activityRepository;
    @Mock private UnavailabilityRepository unavailabilityRepository;
    @Mock private ActivityVoteRepository activityVoteRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private EmailService emailService;
    @Mock private GoogleCalendarService googleCalendarService;

    @InjectMocks
    private ActivityService activityService;

    private Activity testActivity;
    private User testUser;
    private Group testGroup;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testGroup = new Group();
        testGroup.setGroupId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@test.com");

        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setName("Test Activity");
        testActivity.setMinSize(1);
        testActivity.setMaxSize(5);
        testActivity.setDuration(2);
        testActivity.setStatus(ActivityStatus.PENDING);
        testActivity.setGroup(testGroup);
        testActivity.setStartTime(LocalTime.of(8, 0));
        testActivity.setEndTime(LocalTime.of(22, 0));

        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityVoteRepository.existsByActivityIdAndUserId(any(), any())).thenReturn(false);
        when(activityVoteRepository.save(any())).thenReturn(new ActivityVote());
    }


    @Test
    public void createActivity_groupNotFound_throwsNotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());
        Activity a = new Activity();
        a.setName("Test");
        a.setMinSize(1);
        a.setMaxSize(3);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.createActivity(a, 1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void createActivity_userNotFound_throwsNotFound() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        Activity a = new Activity();
        a.setName("Test");
        a.setMinSize(1);
        a.setMaxSize(3);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.createActivity(a, 99L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void createActivity_emptyName_throwsBadRequest() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Activity a = new Activity();
        a.setName("");
        a.setMinSize(1);
        a.setMaxSize(3);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.createActivity(a, 1L, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createActivity_blankName_throwsBadRequest() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Activity a = new Activity();
        a.setName("   ");
        a.setMinSize(1);
        a.setMaxSize(3);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.createActivity(a, 1L, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createActivity_nullName_throwsBadRequest() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Activity a = new Activity();
        a.setName(null);
        a.setMinSize(1);
        a.setMaxSize(3);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.createActivity(a, 1L, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createActivity_minSizeGreaterThanMax_throwsBadRequest() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Activity a = new Activity();
        a.setName("Valid");
        a.setMinSize(10);
        a.setMaxSize(5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.createActivity(a, 1L, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createActivity_minSizeLessThanOne_throwsBadRequest() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Activity a = new Activity();
        a.setName("Valid");
        a.setMinSize(0);
        a.setMaxSize(5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.createActivity(a, 1L, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createActivity_startTimeAfterEndTime_throwsBadRequest() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Activity a = new Activity();
        a.setName("Valid");
        a.setMinSize(1);
        a.setMaxSize(5);
        a.setStartTime(LocalTime.of(18, 0));
        a.setEndTime(LocalTime.of(10, 0));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.createActivity(a, 1L, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createActivity_statusSetToPending() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.save(any(Activity.class))).thenAnswer(i -> i.getArgument(0));

        Activity a = new Activity();
        a.setName("Valid");
        a.setMinSize(1);
        a.setMaxSize(5);

        Activity created = activityService.createActivity(a, 1L, 1L);
        assertEquals(ActivityStatus.PENDING, created.getStatus());
    }

    @Test
    public void createActivity_groupIsAssigned() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.save(any(Activity.class))).thenAnswer(i -> i.getArgument(0));

        Activity a = new Activity();
        a.setName("Valid");
        a.setMinSize(1);
        a.setMaxSize(5);

        Activity created = activityService.createActivity(a, 1L, 1L);
        assertEquals(testGroup, created.getGroup());
    }

    @Test
    public void createActivity_creatorIsAssigned() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.save(any(Activity.class))).thenAnswer(i -> i.getArgument(0));

        Activity a = new Activity();
        a.setName("Valid");
        a.setMinSize(1);
        a.setMaxSize(5);

        Activity created = activityService.createActivity(a, 1L, 1L);
        assertEquals(testUser, created.getCreatedBy());
    }


    @Test
    public void getActivities_groupNotFound_throwsNotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.getActivities(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getActivities_returnsOnlyScheduled() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.SCHEDULED))
            .thenReturn(List.of(testActivity));

        List<Activity> result = activityService.getActivities(1L);
        assertEquals(1, result.size());
    }


    @Test
    public void getActivitiesByStatus_PAST_returnsCorrectList() {
        testActivity.setStatus(ActivityStatus.PAST);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.PAST))
            .thenReturn(List.of(testActivity));

        List<Activity> result = activityService.getActivitiesByStatus(1L, ActivityStatus.PAST);
        assertEquals(1, result.size());
        assertEquals(ActivityStatus.PAST, result.get(0).getStatus());
    }

    @Test
    public void getActivitiesByStatus_FAILED_returnsCorrectList() {
        testActivity.setStatus(ActivityStatus.FAILED);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.FAILED))
            .thenReturn(List.of(testActivity));

        List<Activity> result = activityService.getActivitiesByStatus(1L, ActivityStatus.FAILED);
        assertEquals(1, result.size());
    }

    @Test
    public void getActivitiesByStatus_groupNotFound_throwsNotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.getActivitiesByStatus(99L, ActivityStatus.PAST));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }


    @Test
    public void vote_activityNotFound_throwsNotFound() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.vote(1L, 999L, true, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void vote_wrongGroup_throwsBadRequest() {
        Group other = new Group();
        other.setGroupId(99L);
        testActivity.setGroup(other);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.vote(1L, 1L, true, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void vote_alreadyVotedPendingActivity_throwsConflict() {
        when(activityVoteRepository.existsByActivityIdAndUserId(1L, 1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.vote(1L, 1L, true, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void vote_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.vote(1L, 1L, true, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void vote_saveIsCalledOnce() {
        when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(0L);

        activityService.vote(1L, 1L, false, 1L);
        verify(activityVoteRepository, times(1)).save(any(ActivityVote.class));
    }

    @Test
    public void vote_wantsToJoinFalse_noScheduleAttempt() {
        activityService.vote(1L, 1L, false, 1L);

        verify(activityRepository, never()).save(argThat(a -> a.getStatus() == ActivityStatus.SCHEDULED));
    }

    @Test
    public void vote_scheduledActivity_fullCapacity_throwsConflict() {
        testActivity.setStatus(ActivityStatus.SCHEDULED);
        testActivity.setMaxSize(2);
        when(activityVoteRepository.existsByActivityIdAndUserId(1L, 1L)).thenReturn(false);
        when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(2L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.vote(1L, 1L, true, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }


    @Test
    public void reviveActivity_notFound_throwsNotFound() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.reviveActivity(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void reviveActivity_setsStatusToPending() {
        testActivity.setStatus(ActivityStatus.FAILED);
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityService.reviveActivity(1L);

        assertEquals(ActivityStatus.PENDING, testActivity.getStatus());
    }

    @Test
    public void reviveActivity_clearsScheduledTime() {
        testActivity.setScheduledTime(LocalDateTime.now());
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityService.reviveActivity(1L);

        assertNull(testActivity.getScheduledTime());
    }

    @Test
    public void reviveActivity_deletesExistingVotes() {
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityService.reviveActivity(1L);

        verify(activityVoteRepository, times(1)).deleteByActivityId(1L);
    }


    @Test
    public void getGroupCalendar_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("bad-token")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.getGroupCalendar(1L, "bad-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void getGroupCalendar_groupNotFound_throwsNotFound() {
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.getGroupCalendar(99L, "valid-token"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getGroupCalendar_notMember_throwsForbidden() {
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.getGroupCalendar(1L, "valid-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void getGroupCalendar_member_returnsScheduled() {
        GroupMember member = new GroupMember();
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(member);
        when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.SCHEDULED))
            .thenReturn(List.of(testActivity));

        List<Activity> result = activityService.getGroupCalendar(1L, "valid-token");
        assertEquals(1, result.size());
    }


    @Test
    public void getProposedActivitiesByGroupId_groupNotFound_throwsNotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> activityService.getProposedActivitiesByGroupId(99L, null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getProposedActivitiesByGroupId_returnsOnlyPending() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.PENDING))
            .thenReturn(List.of(testActivity));

        List<Activity> result = activityService.getProposedActivitiesByGroupId(1L, null);
        assertEquals(1, result.size());
        assertEquals(ActivityStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    public void getProposedActivitiesByGroupId_emptyGroup_returnsEmptyList() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.PENDING))
            .thenReturn(Collections.emptyList());

        List<Activity> result = activityService.getProposedActivitiesByGroupId(1L, null);
        assertTrue(result.isEmpty());
    }


    @Test
    public void markPastActivities_pastActivity_markedAsPast() {
        testActivity.setStatus(ActivityStatus.SCHEDULED);
        testActivity.setScheduledTime(LocalDateTime.now().minusDays(2));
        testActivity.setDuration(1);

        when(activityRepository.findByStatus(ActivityStatus.SCHEDULED)).thenReturn(List.of(testActivity));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityService.markPastActivities();

        assertEquals(ActivityStatus.PAST, testActivity.getStatus());
    }

    @Test
    public void markPastActivities_futureActivity_notMarkedAsPast() {
        testActivity.setStatus(ActivityStatus.SCHEDULED);
        testActivity.setScheduledTime(LocalDateTime.now().plusDays(2));
        testActivity.setDuration(1);

        when(activityRepository.findByStatus(ActivityStatus.SCHEDULED)).thenReturn(List.of(testActivity));

        activityService.markPastActivities();

        assertEquals(ActivityStatus.SCHEDULED, testActivity.getStatus());
        verify(activityRepository, never()).save(any());
    }

    @Test
    public void markPastActivities_nullScheduledTime_skipped() {
        testActivity.setStatus(ActivityStatus.SCHEDULED);
        testActivity.setScheduledTime(null);

        when(activityRepository.findByStatus(ActivityStatus.SCHEDULED)).thenReturn(List.of(testActivity));

        activityService.markPastActivities();

        assertEquals(ActivityStatus.SCHEDULED, testActivity.getStatus());
        verify(activityRepository, never()).save(any());
    }

    @Test
    public void markPastActivities_noScheduledActivities_nothingHappens() {
        when(activityRepository.findByStatus(ActivityStatus.SCHEDULED)).thenReturn(Collections.emptyList());

        activityService.markPastActivities();

        verify(activityRepository, never()).save(any());
    }


    @Test
    public void vote_minimumReached_noConflict_emailSent() {
        testActivity.setMinSize(1);
        testActivity.setMaxSize(5);
        testActivity.setDuration(1);
        testActivity.setWeatherDependent(false);
        testActivity.setStartTime(LocalTime.of(8, 0));
        testActivity.setEndTime(LocalTime.of(22, 0));

        ActivityVote v = new ActivityVote();
        v.setUser(testUser);
        v.setWantsToJoin(true);

        when(activityVoteRepository.findByActivityId(1L)).thenReturn(List.of(v));
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(1L);
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityService.vote(1L, 1L, true, 1L);

        verify(emailService, times(1)).sendActivityScheduledEmail(any(), any());
    }
}