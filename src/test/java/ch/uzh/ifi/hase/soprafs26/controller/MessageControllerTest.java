package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Message;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MessagePostDTO;
import ch.uzh.ifi.hase.soprafs26.service.MessageService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
public class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    private String asJson(Object o) throws Exception {
        return new ObjectMapper().writeValueAsString(o);
    }

    private Message buildMessage(Long id, String text, String senderUsername) {
        User sender = new User();
        sender.setId(1L);
        sender.setUsername(senderUsername);

        Message m = new Message();
        m.setId(id);
        m.setText(text);
        m.setSender(sender);
        m.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        return m;
    }


    @Test
    public void getMessages_valid_returns200WithList() throws Exception {
        Message m = buildMessage(1L, "Hello!", "alice");

        given(messageService.getMessages(eq(1L), eq("alice-token"))).willReturn(List.of(m));

        mockMvc.perform(get("/groups/1/messages")
                .header("Authorization", "alice-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(1)))
            .andExpect(jsonPath("$[0].text", is("Hello!")))
            .andExpect(jsonPath("$[0].senderName", is("alice")));
    }

    @Test
    public void getMessages_emptyGroup_returnsEmptyArray() throws Exception {
        given(messageService.getMessages(eq(1L), anyString())).willReturn(Collections.emptyList());

        mockMvc.perform(get("/groups/1/messages")
                .header("Authorization", "alice-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getMessages_notLoggedIn_returns401() throws Exception {
        given(messageService.getMessages(anyLong(), anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(get("/groups/1/messages")
                .header("Authorization", "bad-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getMessages_notMember_returns403() throws Exception {
        given(messageService.getMessages(anyLong(), anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/groups/1/messages")
                .header("Authorization", "outsider-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void getMessages_groupNotFound_returns404() throws Exception {
        given(messageService.getMessages(eq(99L), anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/groups/99/messages")
                .header("Authorization", "alice-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getMessages_multipleMessages_allReturned() throws Exception {
        Message m1 = buildMessage(1L, "First", "alice");
        Message m2 = buildMessage(2L, "Second", "bob");

        given(messageService.getMessages(eq(1L), anyString())).willReturn(List.of(m1, m2));

        mockMvc.perform(get("/groups/1/messages")
                .header("Authorization", "alice-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].text", is("First")))
            .andExpect(jsonPath("$[1].text", is("Second")));
    }

    @Test
    public void getMessages_createdAtIsReturned() throws Exception {
        Message m = buildMessage(1L, "Hi", "alice");

        given(messageService.getMessages(eq(1L), anyString())).willReturn(List.of(m));

        mockMvc.perform(get("/groups/1/messages")
                .header("Authorization", "alice-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].createdAt", notNullValue()));
    }


    @Test
    public void sendMessage_valid_returns201WithMessage() throws Exception {
        Message saved = buildMessage(5L, "Hello group!", "alice");

        given(messageService.sendMessage(eq(1L), eq("Hello group!"), anyString())).willReturn(saved);

        MessagePostDTO dto = new MessagePostDTO();
        dto.setText("Hello group!");

        mockMvc.perform(post("/groups/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "alice-token")
                .content(asJson(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(5)))
            .andExpect(jsonPath("$.text", is("Hello group!")))
            .andExpect(jsonPath("$.senderName", is("alice")));
    }

    @Test
    public void sendMessage_emptyText_returns400() throws Exception {
        given(messageService.sendMessage(anyLong(), anyString(), anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty"));

        MessagePostDTO dto = new MessagePostDTO();
        dto.setText("");

        mockMvc.perform(post("/groups/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "alice-token")
                .content(asJson(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void sendMessage_notLoggedIn_returns401() throws Exception {
        given(messageService.sendMessage(anyLong(), anyString(), anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        MessagePostDTO dto = new MessagePostDTO();
        dto.setText("Hi");

        mockMvc.perform(post("/groups/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "bad-token")
                .content(asJson(dto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void sendMessage_notMember_returns403() throws Exception {
        given(messageService.sendMessage(anyLong(), anyString(), anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        MessagePostDTO dto = new MessagePostDTO();
        dto.setText("Hi");

        mockMvc.perform(post("/groups/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "outsider-token")
                .content(asJson(dto)))
            .andExpect(status().isForbidden());
    }

    @Test
    public void sendMessage_groupNotFound_returns404() throws Exception {
        given(messageService.sendMessage(eq(99L), anyString(), anyString()))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        MessagePostDTO dto = new MessagePostDTO();
        dto.setText("Hi");

        mockMvc.perform(post("/groups/99/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "alice-token")
                .content(asJson(dto)))
            .andExpect(status().isNotFound());
    }

    @Test
    public void sendMessage_createdAtIsReturned() throws Exception {
        Message saved = buildMessage(1L, "Hi", "alice");
        given(messageService.sendMessage(anyLong(), anyString(), anyString())).willReturn(saved);

        MessagePostDTO dto = new MessagePostDTO();
        dto.setText("Hi");

        mockMvc.perform(post("/groups/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "alice-token")
                .content(asJson(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdAt", notNullValue()));
    }
}