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

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;



public class ActivityServiceTest {

	@Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityVoteRepository activityVoteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;


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


@Test


@Test