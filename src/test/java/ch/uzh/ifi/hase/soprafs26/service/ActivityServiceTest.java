package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import ch.uzh.ifi.hase.soprafs26.repository.UnavailabilityRepository;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMemberRepository;



import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import java.util.List;



import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;


public class ActivityServiceTest {

	@Mock
    private ActivityRepository activityRepository;

    @Mock
    private UnavailabilityRepository unavailabilityRepository;

    @Mock
    private ActivityVoteRepository activityVoteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

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

        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setName("Test Activity");
        testActivity.setMinSize(2);
        testActivity.setMaxSize(5);
        testActivity.setStatus(ActivityStatus.PENDING);
        testActivity.setGroup(testGroup);

        Mockito.when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        Mockito.when(activityVoteRepository.existsByActivityIdAndUserId(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(activityVoteRepository.save(Mockito.any())).thenReturn(new ActivityVote());
	}

    @Test
    public void createActivity_success() {
        Activity activityInput = new Activity();
        activityInput.setName("Pizza Night");
        activityInput.setMinSize(1);
        activityInput.setMaxSize(5);
        
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        Mockito.when(activityRepository.save(Mockito.any(Activity.class))).thenAnswer(i -> i.getArguments()[0]);
    
        Activity created = activityService.createActivity(activityInput, 1L, 1L);
    
        assertNotNull(created);
        assertEquals("Pizza Night", created.getName());
        assertEquals(ActivityStatus.PENDING, created.getStatus());
        assertEquals(testGroup, created.getGroup());
    }
    
    @Test
    public void testVote_wrongGroup_throwsBadRequest() {
        Group differentGroup = new Group();
        differentGroup.setGroupId(99L);
        testActivity.setGroup(differentGroup);
    
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            activityService.vote(1L, 1L, true, 1L);
        });
    
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Activity does not belong to this group", exception.getReason());
    }
    
    @Test
    public void testGetProposedActivities_success() {
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.PENDING))
               .thenReturn(java.util.List.of(testActivity));
    
        var result = activityService.getProposedActivitiesByGroupId(1L);
    
        assertEquals(1, result.size());
        assertEquals(ActivityStatus.PENDING, result.get(0).getStatus());
    }
    
    @Test
    public void testGetGroupCalendar_notMember_throwsForbidden() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(groupMemberRepository.findByGroupAndUser(Mockito.any(), Mockito.any())).thenReturn(null);
    
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            activityService.getGroupCalendar(1L, "valid-token");
        });
    
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
    
    @Test
    public void testVote_activityNotFound_throwsNotFound() {
        Mockito.when(activityRepository.findById(1L)).thenReturn(Optional.empty());
    
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            activityService.vote(1L, 1L, true, 1L);
        });
    
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

@Test
public void testVoteSuccess() {
    Mockito.when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(1L);
    Mockito.when(activityRepository.save(Mockito.any())).thenReturn(testActivity);

    activityService.vote(1L,1L,true,1L);
    System.out.println("testVoteSuccess - The status is" + testActivity.getStatus());
    assertEquals(ActivityStatus.PENDING, testActivity.getStatus());
}

@Test
public void testVoteRecordSaved() {
    activityService.vote(1L, 1L, true, 1L);
    Mockito.verify(activityVoteRepository, Mockito.times(1)).save(Mockito.any(ActivityVote.class));
}

@Test
public void testMinimumNotReached() {
    Mockito.when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(1L);
    Mockito.when(activityRepository.save(Mockito.any())).thenReturn(testActivity);
    activityService.vote(1L,1L,true,1L);

    System.out.println("testVoteSuccess - The status is" + testActivity.getStatus());
    assertEquals(ActivityStatus.PENDING, testActivity.getStatus());

    Mockito.verify(activityRepository, Mockito.never()).save(Mockito.argThat(testing->testing.getStatus()
    == ActivityStatus.SCHEDULED));
    System.out.println("The scheduling has not been triggered because the minium participants count is not yet met");
}

@Test
public void testTriggerSearch() {
    Mockito.when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(2L);
    Mockito.when(activityVoteRepository.findByActivityId(1L)).thenReturn(new ArrayList<>());
    Mockito.when(activityRepository.save(Mockito.any())).thenReturn(testActivity);

    activityService.vote(1L,1L,true,1L);
    System.out.println("testTriggerSearch - The status is" + testActivity.getStatus());
    assertEquals(ActivityStatus.SCHEDULED, testActivity.getStatus());
}

@Test
public void testAlreadyVoted() {
Mockito.when(activityVoteRepository.existsByActivityIdAndUserId(1L,1L)).thenReturn(true);

ResponseStatusException fail = assertThrows(ResponseStatusException.class, () -> {
    activityService.vote(1L, 1L, true, 1L);
});
System.out.println("TestAlreadyVoted - The status is" + fail.getStatusCode());
    assertEquals(409, fail.getStatusCode().value());


}

@Test
public void testActivityNotFound() {
    Mockito.when(activityRepository.findById(99L)).thenReturn(Optional.empty());
    ResponseStatusException fail = assertThrows(ResponseStatusException.class, () -> {activityService.vote(1L,99L,true, 1L);
});
System.out.println("testActivityNotFound - Result status:" + fail.getStatusCode());
assertEquals(404, fail.getStatusCode().value()); 
}

@Test
public void testRecursiveActivity_createsNewPendingActivity() {
    testActivity.setRecursive(true);
    testActivity.setName("Recurring Hike");
    testActivity.setMinSize(2);
    testActivity.setDuration(1);

    Mockito.when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(2L);
    Mockito.when(activityVoteRepository.findByActivityId(1L)).thenReturn(new ArrayList<>());
    Mockito.when(unavailabilityRepository.findByUserId(Mockito.any())).thenReturn(new ArrayList<>());
    Mockito.when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.PENDING))
        .thenReturn(new ArrayList<>());
    Mockito.when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.SCHEDULED))
        .thenReturn(new ArrayList<>());
    Mockito.when(activityRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

    activityService.vote(1L, 1L, true, 1L);

    Mockito.verify(activityRepository, Mockito.times(2)).save(Mockito.argThat(a ->
        a.getName().equals("Recurring Hike")
    ));
}

@Test
public void testRecursiveActivity_doesNotDuplicate_whenScheduledExists() {
    testActivity.setRecursive(true);
    testActivity.setName("Recurring Hike");
    testActivity.setId(1L);
    testActivity.setMinSize(2);
    testActivity.setDuration(1);

    Activity otherScheduled = new Activity();
    otherScheduled.setName("Recurring Hike");
    otherScheduled.setStatus(ActivityStatus.SCHEDULED);
    otherScheduled.setId(99L);

    Mockito.when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(2L);
    Mockito.when(activityVoteRepository.findByActivityId(1L)).thenReturn(new ArrayList<>());
    Mockito.when(unavailabilityRepository.findByUserId(Mockito.any())).thenReturn(new ArrayList<>());
    Mockito.when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.PENDING))
        .thenReturn(new ArrayList<>());
    Mockito.when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.SCHEDULED))
        .thenReturn(List.of(otherScheduled));
    Mockito.when(activityRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

    activityService.vote(1L, 1L, true, 1L);

    Mockito.verify(activityRepository, Mockito.never()).save(Mockito.argThat(a ->
        a.getStatus() == ActivityStatus.PENDING
    ));
}

@Test
public void testNonRecursiveActivity_doesNotCreateNewActivity() {
    testActivity.setRecursive(false);
    testActivity.setName("One Time Event");
    testActivity.setMinSize(2);
    testActivity.setDuration(1);

    Mockito.when(activityVoteRepository.countByActivityIdAndWantsToJoinTrue(1L)).thenReturn(2L);
    Mockito.when(activityVoteRepository.findByActivityId(1L)).thenReturn(new ArrayList<>());
    Mockito.when(unavailabilityRepository.findByUserId(Mockito.any())).thenReturn(new ArrayList<>());
    Mockito.when(activityRepository.findByGroupGroupIdAndStatus(1L, ActivityStatus.PENDING))
        .thenReturn(new ArrayList<>());
    Mockito.when(activityRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

    activityService.vote(1L, 1L, true, 1L);

    Mockito.verify(activityRepository, Mockito.times(1)).save(Mockito.any());
}

}