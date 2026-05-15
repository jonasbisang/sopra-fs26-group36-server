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

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GroupMemberRepository groupMemberRepository;

    private String asJson(Object o) throws Exception {
        return new ObjectMapper().writeValueAsString(o);
    }


    @Test
    public void login_validCredentials_returnsUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setStatus(UserStatus.ONLINE);
        user.setToken("new-token");

        given(userService.loginUser(Mockito.any())).willReturn(user);

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("alice");
        dto.setPassword("pass");

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("alice")))
            .andExpect(jsonPath("$.status", is("ONLINE")));
    }

    @Test
    public void login_wrongCredentials_returns401() throws Exception {
        given(userService.loginUser(Mockito.any()))
            .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("alice");
        dto.setPassword("wrong");

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(dto)))
            .andExpect(status().isUnauthorized());
    }


    @Test
    public void logout_valid_returns200() throws Exception {
        doNothing().when(userService).logoutUser("valid-token");

        mockMvc.perform(post("/users/logout")
                .header("Authorization", "valid-token"))
            .andExpect(status().isOk());
    }

    @Test
    public void logout_invalidToken_returns401() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED))
            .when(userService).logoutUser("bad-token");

        mockMvc.perform(post("/users/logout")
                .header("Authorization", "bad-token"))
            .andExpect(status().isUnauthorized());
    }


    @Test
    public void createUser_missingEmail_returns409() throws Exception {
        given(userService.createUser(Mockito.any()))
            .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email required"));

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("bob");
        dto.setPassword("pass");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(dto)))
            .andExpect(status().isBadRequest());
    }


    @Test
    public void changeBio_valid_returns204() throws Exception {
        doNothing().when(userService).changeBio(anyLong(), anyString(), anyString());

        UserPutDTO dto = new UserPutDTO();
        dto.setNewBio("I love hiking");

        mockMvc.perform(put("/users/1/bio")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "valid-token")
                .content(asJson(dto)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void changeBio_notOwner_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
            .when(userService).changeBio(anyLong(), anyString(), anyString());

        UserPutDTO dto = new UserPutDTO();
        dto.setNewBio("Hacker bio");

        mockMvc.perform(put("/users/1/bio")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "wrong-token")
                .content(asJson(dto)))
            .andExpect(status().isForbidden());
    }


    @Test
    public void changePassword_valid_returns204() throws Exception {
        doNothing().when(userService).changePassword(anyLong(), anyString(), anyString());

        UserPutDTO dto = new UserPutDTO();
        dto.setOldPassword("old");
        dto.setNewPassword("new");

        mockMvc.perform(put("/users/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(dto)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void changePassword_wrongOld_returns401() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED))
            .when(userService).changePassword(anyLong(), anyString(), anyString());

        UserPutDTO dto = new UserPutDTO();
        dto.setOldPassword("wrong");
        dto.setNewPassword("new");

        mockMvc.perform(put("/users/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(dto)))
            .andExpect(status().isUnauthorized());
    }


    @Test
    public void deleteUser_valid_returns204() throws Exception {
        doNothing().when(userService).deleteUser(anyLong(), anyString(), anyString());

        UserPutDTO dto = new UserPutDTO();
        dto.setOldPassword("pass");

        mockMvc.perform(delete("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "valid-token")
                .content(asJson(dto)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void deleteUser_wrongPassword_returns401() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED))
            .when(userService).deleteUser(anyLong(), anyString(), anyString());

        UserPutDTO dto = new UserPutDTO();
        dto.setOldPassword("wrong");

        mockMvc.perform(delete("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "valid-token")
                .content(asJson(dto)))
            .andExpect(status().isUnauthorized());
    }


    @Test
    public void getUserById_valid_returnsUser() throws Exception {
        User user = new User();
        user.setId(5L);
        user.setUsername("charlie");
        user.setStatus(UserStatus.OFFLINE);

        given(userService.getUserById(5L)).willReturn(user);

        mockMvc.perform(get("/users/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(5)))
            .andExpect(jsonPath("$.username", is("charlie")));
    }


    @Test
    public void getAllUsers_emptyList_returnsEmptyArray() throws Exception {
        given(userService.getUsers()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getAllUsers_multipleUsers_returnsAll() throws Exception {
        User u1 = new User();
        u1.setUsername("alice");
        u1.setStatus(UserStatus.ONLINE);

        User u2 = new User();
        u2.setUsername("bob");
        u2.setStatus(UserStatus.OFFLINE);

        given(userService.getUsers()).willReturn(List.of(u1, u2));

        mockMvc.perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }
}

