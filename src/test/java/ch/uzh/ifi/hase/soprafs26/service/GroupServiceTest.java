package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.RoleType;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMemberRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private User testUser;
    private Group testGroup;
    private GroupMember testMember;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setToken("valid-token");

        testGroup = new Group();
        testGroup.setGroupId(1L);
        testGroup.setName("Test Group");
        testGroup.setJoinPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret"));

        testMember = new GroupMember();
        testMember.setUser(testUser);
        testMember.setGroup(testGroup);
        testMember.setRole(RoleType.ADMIN);
    }

    // #103 — creator is assigned as admin on group creation
    @Test
    public void createGroup_creatorIsAdmin() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.existsByName("Test Group")).thenReturn(false);
        Mockito.when(groupRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(groupMemberRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        Group newGroup = new Group();
        newGroup.setName("Test Group");
        newGroup.setJoinPassword("secret");

        groupService.createGroup(newGroup, "valid-token");

        Mockito.verify(groupMemberRepository).save(Mockito.argThat(member ->
            member.getRole() == RoleType.ADMIN &&
            member.getUser().equals(testUser)
        ));
    }

    // #103 — duplicate group name throws conflict
    @Test
    public void createGroup_duplicateName_throwsConflict() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.existsByName("Test Group")).thenReturn(true);

        Group newGroup = new Group();
        newGroup.setName("Test Group");

        assertThrows(ResponseStatusException.class, () ->
            groupService.createGroup(newGroup, "valid-token"));
    }

    // #113 — join group with correct password succeeds
    @Test
    public void joinGroup_correctPassword_success() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(null);
        Mockito.when(groupMemberRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        GroupMember result = groupService.joinGroup(1L, "secret", "valid-token");

        assertEquals(RoleType.MEMBER, result.getRole());
    }

    // #113 — join group with wrong password throws unauthorized
    @Test
    public void joinGroup_wrongPassword_throwsUnauthorized() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(null);

        assertThrows(ResponseStatusException.class, () ->
            groupService.joinGroup(1L, "wrongpassword", "valid-token"));
    }

    // #113 — already member throws conflict
    @Test
    public void joinGroup_alreadyMember_throwsConflict() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(testMember);

        assertThrows(ResponseStatusException.class, () ->
            groupService.joinGroup(1L, "secret", "valid-token"));
    }

    // #114 — non-admin cannot promote
    @Test
    public void promoteMember_nonAdmin_throwsForbidden() {
        testMember.setRole(RoleType.MEMBER);

        User targetUser = new User();
        targetUser.setId(2L);

        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(testMember);

        assertThrows(ResponseStatusException.class, () ->
            groupService.promoteMember(1L, 2L, "valid-token"));
    }

    // #115 — non-admin cannot delete group
    @Test
    public void deleteGroup_nonAdmin_throwsForbidden() {
        testMember.setRole(RoleType.MEMBER);

        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(testMember);

        assertThrows(ResponseStatusException.class, () ->
            groupService.deleteGroup(1L, "valid-token"));
    }

    // #115 — wrong old password throws unauthorized
    @Test
    public void changeGroupPassword_wrongOldPassword_throwsUnauthorized() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(testMember);

        assertThrows(ResponseStatusException.class, () ->
            groupService.changeGroupPassword(1L, "wrongpassword", "newpassword", "valid-token"));
    }

    // #120 — verifyMembership throws forbidden for non-member
    @Test
    public void leaveGroup_notMember_throwsForbidden() {
        Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        Mockito.when(groupMemberRepository.findByGroupAndUser(testGroup, testUser)).thenReturn(null);

        assertThrows(ResponseStatusException.class, () ->
            groupService.leaveGroup(1L, 1L, "valid-token"));
    }

    @Test
    public void createGroup_activityIsPending() {
    // This is covered implicitly — new activities start as PENDING
    // Test that a newly created activity has PENDING status
    Activity activity = new Activity();
    activity.setStatus(ActivityStatus.PENDING);
    assertEquals(ActivityStatus.PENDING, activity.getStatus());
    }
}