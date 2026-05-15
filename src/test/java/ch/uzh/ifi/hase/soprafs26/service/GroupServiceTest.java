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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private User admin;
    private User member;
    private Group group;
    private GroupMember adminMembership;
    private GroupMember memberMembership;

    @BeforeEach
    public void setup() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setToken("admin-token");

        member = new User();
        member.setId(2L);
        member.setUsername("member");
        member.setToken("member-token");

        group = new Group();
        group.setGroupId(1L);
        group.setName("Test Group");
        group.setJoinPassword(encoder.encode("secret"));

        adminMembership = new GroupMember();
        adminMembership.setUser(admin);
        adminMembership.setGroup(group);
        adminMembership.setRole(RoleType.ADMIN);

        memberMembership = new GroupMember();
        memberMembership.setUser(member);
        memberMembership.setGroup(group);
        memberMembership.setRole(RoleType.MEMBER);
    }

    // --- createGroup ---

    @Test
    public void createGroup_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);
        Group g = new Group();
        g.setName("X");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.createGroup(g, "bad"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void createGroup_emptyName_throwsBadRequest() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        Group g = new Group();
        g.setName("  ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.createGroup(g, "admin-token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createGroup_nullName_throwsBadRequest() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        Group g = new Group();
        g.setName(null);

        assertThrows(ResponseStatusException.class,
            () -> groupService.createGroup(g, "admin-token"));
    }

    @Test
    public void createGroup_duplicateName_throwsConflict() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.existsByName("Test Group")).thenReturn(true);
        Group g = new Group();
        g.setName("Test Group");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.createGroup(g, "admin-token"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void createGroup_passwordIsHashed() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.existsByName("New Group")).thenReturn(false);
        when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(groupMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Group g = new Group();
        g.setName("New Group");
        g.setJoinPassword("mypassword");

        Group created = groupService.createGroup(g, "admin-token");

        assertNotEquals("mypassword", created.getJoinPassword());
        assertTrue(encoder.matches("mypassword", created.getJoinPassword()));
    }

    @Test
    public void createGroup_creatorSavedAsAdmin() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.existsByName("New Group")).thenReturn(false);
        when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(groupMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Group g = new Group();
        g.setName("New Group");

        groupService.createGroup(g, "admin-token");

        verify(groupMemberRepository).save(argThat(gm ->
            gm.getRole() == RoleType.ADMIN && gm.getUser().equals(admin)
        ));
    }


    @Test
    public void joinGroup_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> groupService.joinGroup(1L, "secret", "bad"));
    }

    @Test
    public void joinGroup_groupNotFound_throwsNotFound() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.joinGroup(99L, "secret", "member-token"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void joinGroup_alreadyMember_throwsConflict() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.joinGroup(1L, "secret", "member-token"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void joinGroup_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.joinGroup(1L, "wrong", "member-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void joinGroup_correctPassword_roleIsMember() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(null);
        when(groupMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GroupMember gm = groupService.joinGroup(1L, "secret", "member-token");
        assertEquals(RoleType.MEMBER, gm.getRole());
    }

    @Test
    public void joinGroup_noPassword_success() {
        group.setJoinPassword(null);
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(null);
        when(groupMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GroupMember gm = groupService.joinGroup(1L, null, "member-token");
        assertEquals(RoleType.MEMBER, gm.getRole());
    }


    @Test
    public void leaveGroup_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> groupService.leaveGroup(1L, 2L, "bad"));
    }

    @Test
    public void leaveGroup_groupNotFound_throwsNotFound() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> groupService.leaveGroup(99L, 2L, "member-token"));
    }

    @Test
    public void leaveGroup_notMember_throwsNotFound() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> groupService.leaveGroup(1L, 2L, "member-token"));
    }

    @Test
    public void leaveGroup_soleAdmin_throwsBadRequest() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupMemberRepository.findByGroupAndRole(group, RoleType.ADMIN)).thenReturn(List.of(adminMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.leaveGroup(1L, 1L, "admin-token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void leaveGroup_memberLeaves_success() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);

        groupService.leaveGroup(1L, 2L, "member-token");

        verify(groupMemberRepository, times(1)).delete(memberMembership);
    }

    @Test
    public void leaveGroup_adminWithOtherAdmin_canLeave() {
        User admin2 = new User();
        admin2.setId(3L);
        admin2.setToken("admin2-token");
        GroupMember admin2Membership = new GroupMember();
        admin2Membership.setUser(admin2);
        admin2Membership.setGroup(group);
        admin2Membership.setRole(RoleType.ADMIN);

        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupMemberRepository.findByGroupAndRole(group, RoleType.ADMIN))
            .thenReturn(List.of(adminMembership, admin2Membership));

        groupService.leaveGroup(1L, 1L, "admin-token");

        verify(groupMemberRepository, times(1)).delete(adminMembership);
    }


    @Test
    public void kickMember_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> groupService.kickMember(1L, 2L, "bad"));
    }

    @Test
    public void kickMember_nonAdmin_throwsForbidden() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.kickMember(1L, 1L, "member-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void kickMember_kickingAdmin_throwsForbidden() {
        User admin2 = new User();
        admin2.setId(3L);
        GroupMember admin2Membership = new GroupMember();
        admin2Membership.setUser(admin2);
        admin2Membership.setGroup(group);
        admin2Membership.setRole(RoleType.ADMIN);

        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin2));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupMemberRepository.findByGroupAndUser(group, admin2)).thenReturn(admin2Membership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.kickMember(1L, 3L, "admin-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void kickMember_targetNotInGroup_throwsNotFound() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.kickMember(1L, 2L, "admin-token"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void kickMember_valid_memberDeleted() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);

        groupService.kickMember(1L, 2L, "admin-token");

        verify(groupMemberRepository, times(1)).delete(memberMembership);
    }


    @Test
    public void promoteMember_targetNotInGroup_throwsNotFound() {
        User outsider = new User();
        outsider.setId(5L);
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(userRepository.findById(5L)).thenReturn(Optional.of(outsider));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupMemberRepository.findByGroupAndUser(group, outsider)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.promoteMember(1L, 5L, "admin-token"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void promoteMember_valid_roleBecomesAdmin() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);
        when(groupMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        groupService.promoteMember(1L, 2L, "admin-token");

        assertEquals(RoleType.ADMIN, memberMembership.getRole());
    }


    @Test
    public void deleteGroup_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> groupService.deleteGroup(1L, "bad"));
    }

    @Test
    public void deleteGroup_groupNotFound_throwsNotFound() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> groupService.deleteGroup(99L, "admin-token"));
    }

    @Test
    public void deleteGroup_notMember_throwsNotFound() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.deleteGroup(1L, "admin-token"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void deleteGroup_nonAdmin_throwsForbidden() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.deleteGroup(1L, "member-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void deleteGroup_admin_groupDeleted() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);

        groupService.deleteGroup(1L, "admin-token");

        verify(groupRepository, times(1)).delete(group);
    }


    @Test
    public void changeGroupPassword_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> groupService.changeGroupPassword(1L, "old", "new", "bad"));
    }

    @Test
    public void changeGroupPassword_nonAdmin_throwsForbidden() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.changeGroupPassword(1L, "secret", "new", "member-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void changeGroupPassword_wrongOldPassword_throwsUnauthorized() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.changeGroupPassword(1L, "wrong", "new", "admin-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void changeGroupPassword_emptyNewPassword_throwsBadRequest() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> groupService.changeGroupPassword(1L, "secret", "  ", "admin-token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void changeGroupPassword_valid_passwordIsHashed() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        groupService.changeGroupPassword(1L, "secret", "newpass", "admin-token");

        assertTrue(encoder.matches("newpass", group.getJoinPassword()));
    }


    @Test
    public void getGroupMembers_groupNotFound_throwsNotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> groupService.getGroupMembers(99L, "admin-token"));
    }

    @Test
    public void getGroupMembers_returnsUserList() {
        group.getMembers().add(adminMembership);
        group.getMembers().add(memberMembership);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        List<User> users = groupService.getGroupMembers(1L, "admin-token");

        assertEquals(2, users.size());
    }


    @Test
    public void removeMember_selfRemoval_callsLeaveGroup() {
        when(userRepository.findByToken("member-token")).thenReturn(member);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);

        groupService.removeMember(1L, member.getId(), "member-token");

        verify(groupMemberRepository, times(1)).delete(memberMembership);
    }

    @Test
    public void removeMember_adminKicksOther_callsKickMember() {
        when(userRepository.findByToken("admin-token")).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, admin)).thenReturn(adminMembership);
        when(groupMemberRepository.findByGroupAndUser(group, member)).thenReturn(memberMembership);

        groupService.removeMember(1L, member.getId(), "admin-token");

        verify(groupMemberRepository, times(1)).delete(memberMembership);
    }
}