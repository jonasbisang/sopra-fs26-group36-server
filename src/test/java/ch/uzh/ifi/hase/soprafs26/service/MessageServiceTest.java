package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.entity.Message;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMemberRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.repository.MessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private Group group;
    private GroupMember membership;

    @BeforeEach
    public void setup() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("alice");
        sender.setToken("alice-token");

        group = new Group();
        group.setGroupId(1L);
        group.setName("Test Group");

        membership = new GroupMember();
        membership.setUser(sender);
        membership.setGroup(group);
    }

    @Test
    public void getMessages_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.getMessages(1L, "bad"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void getMessages_groupNotFound_throwsNotFound() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.getMessages(99L, "alice-token"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getMessages_notMember_throwsForbidden() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.getMessages(1L, "alice-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void getMessages_member_returnsMessages() {
        Message m1 = new Message();
        m1.setId(1L);
        m1.setText("Hello");
        m1.setSender(sender);
        m1.setGroup(group);
        m1.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.findByGroupGroupIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(m1));

        List<Message> result = messageService.getMessages(1L, "alice-token");

        assertEquals(1, result.size());
        assertEquals("Hello", result.get(0).getText());
    }

    @Test
    public void getMessages_emptyGroup_returnsEmptyList() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.findByGroupGroupIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());

        List<Message> result = messageService.getMessages(1L, "alice-token");

        assertTrue(result.isEmpty());
    }

    @Test
    public void getMessages_multipleMessages_allReturned() {
        Message m1 = new Message();
        m1.setId(1L);
        m1.setText("First");
        m1.setSender(sender);
        m1.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        Message m2 = new Message();
        m2.setId(2L);
        m2.setText("Second");
        m2.setSender(sender);
        m2.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.findByGroupGroupIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(m1, m2));

        List<Message> result = messageService.getMessages(1L, "alice-token");

        assertEquals(2, result.size());
    }

    @Test
    public void sendMessage_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(1L, "hello", "bad"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void sendMessage_groupNotFound_throwsNotFound() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(99L, "hello", "alice-token"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void sendMessage_notMember_throwsForbidden() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(1L, "hello", "alice-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void sendMessage_emptyText_throwsBadRequest() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(1L, "", "alice-token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void sendMessage_blankText_throwsBadRequest() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(1L, "   ", "alice-token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void sendMessage_nullText_throwsBadRequest() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(1L, null, "alice-token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void sendMessage_valid_returnsMessage() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        Message result = messageService.sendMessage(1L, "Hello group!", "alice-token");

        assertNotNull(result);
        assertEquals("Hello group!", result.getText());
    }

    @Test
    public void sendMessage_valid_senderIsSet() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        Message result = messageService.sendMessage(1L, "Hi!", "alice-token");

        assertEquals(sender, result.getSender());
    }

    @Test
    public void sendMessage_valid_groupIsSet() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        Message result = messageService.sendMessage(1L, "Hi!", "alice-token");

        assertEquals(group, result.getGroup());
    }

    @Test
    public void sendMessage_valid_createdAtIsSet() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        Message result = messageService.sendMessage(1L, "Hi!", "alice-token");

        assertNotNull(result.getCreatedAt());
    }

    @Test
    public void sendMessage_valid_textIsTrimmed() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        Message result = messageService.sendMessage(1L, "  Hello!  ", "alice-token");

        assertEquals("Hello!", result.getText());
    }

    @Test
    public void sendMessage_valid_saveIsCalledOnce() {
        when(userRepository.findByToken("alice-token")).thenReturn(sender);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupAndUser(group, sender)).thenReturn(membership);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        messageService.sendMessage(1L, "Hello!", "alice-token");

        verify(messageRepository, times(1)).save(any(Message.class));
    }
}