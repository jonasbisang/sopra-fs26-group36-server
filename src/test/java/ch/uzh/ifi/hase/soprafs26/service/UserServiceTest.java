package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UnavailabilityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UnavailabilityRepository unavailabilityRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(encoder.encode("password123"));
        testUser.setStatus(UserStatus.OFFLINE);
        testUser.setToken("valid-token");
    }


    @Test
    public void createUser_nullUsername_throwsBadRequest() {
        testUser.setUsername(null);
        testUser.setPassword("pass");
        testUser.setEmail("e@mail.com");

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_blankUsername_throwsBadRequest() {
        testUser.setUsername("  ");
        testUser.setPassword("pass");
        testUser.setEmail("e@mail.com");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.createUser(testUser));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createUser_nullEmail_throwsBadRequest() {
        testUser.setEmail(null);
        testUser.setPassword("pass");
        testUser.setUsername("user123");

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_blankEmail_throwsBadRequest() {
        testUser.setEmail("  ");
        testUser.setPassword("pass");
        testUser.setUsername("user123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.createUser(testUser));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createUser_nullPassword_throwsBadRequest() {
        testUser.setPassword(null);
        testUser.setUsername("user123");
        testUser.setEmail("e@test.com");

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }

    @Test
    public void createUser_tokenAssigned() {
        testUser.setPassword("rawpass");
        testUser.setUsername("newuser");
        testUser.setEmail("new@test.com");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User created = userService.createUser(testUser);
        assertNotNull(created.getToken());
    }

    @Test
    public void createUser_creationDateSet() {
        testUser.setPassword("rawpass");
        testUser.setUsername("newuser");
        testUser.setEmail("new@test.com");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User created = userService.createUser(testUser);
        assertEquals(LocalDate.now(), created.getCreationDate());
    }

    @Test
    public void createUser_statusSetToOffline() {
        testUser.setPassword("rawpass");
        testUser.setUsername("newuser");
        testUser.setEmail("new@test.com");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User created = userService.createUser(testUser);
        assertEquals(UserStatus.OFFLINE, created.getStatus());
    }

    @Test
    public void createUser_bothDuplicates_throwsBadRequest() {
        testUser.setPassword("rawpass");
        testUser.setUsername("dupuser");
        testUser.setEmail("dup@test.com");
        when(userRepository.findByUsername("dupuser")).thenReturn(testUser);
        when(userRepository.findByEmail("dup@test.com")).thenReturn(testUser);

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
    }


    @Test
    public void loginUser_usernameNotFound_throwsUnauthorized() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        User input = new User();
        input.setUsername("unknown");
        input.setPassword("pass");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.loginUser(input));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void loginUser_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);

        User input = new User();
        input.setUsername("testuser");
        input.setPassword("wrongpassword");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.loginUser(input));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void loginUser_success_statusOnline() {
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User input = new User();
        input.setUsername("testuser");
        input.setPassword("password123");

        User loggedIn = userService.loginUser(input);
        assertEquals(UserStatus.ONLINE, loggedIn.getStatus());
    }

    @Test
    public void loginUser_success_newTokenAssigned() {
        String oldToken = testUser.getToken();
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User input = new User();
        input.setUsername("testuser");
        input.setPassword("password123");

        User loggedIn = userService.loginUser(input);
        assertNotNull(loggedIn.getToken());
    }


    @Test
    public void logoutUser_invalidToken_throwsUnauthorized() {
        when(userRepository.findByToken("invalid")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.logoutUser("invalid"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void logoutUser_valid_tokenCleared() {
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.logoutUser("valid-token");

        assertNull(testUser.getToken());
    }

    @Test
    public void logoutUser_valid_statusOffline() {
        testUser.setStatus(UserStatus.ONLINE);
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.logoutUser("valid-token");

        assertEquals(UserStatus.OFFLINE, testUser.getStatus());
    }


    @Test
    public void changeUsername_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> userService.changeUsername(99L, "valid-token", "newuser"));
    }

    @Test
    public void changeUsername_wrongToken_throwsUnauthorized() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.changeUsername(1L, "wrong-token", "newuser"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void changeUsername_taken_throwsConflict() {
        User other = new User();
        other.setUsername("takenname");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("takenname")).thenReturn(other);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.changeUsername(1L, "valid-token", "takenname"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void changeUsername_valid_usernameUpdated() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("brandnew")).thenReturn(null);

        userService.changeUsername(1L, "valid-token", "brandnew");

        assertEquals("brandnew", testUser.getUsername());
    }


    @Test
    public void changePassword_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> userService.changePassword(99L, "old", "new"));
    }

    @Test
    public void changePassword_wrongOldPassword_throwsUnauthorized() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.changePassword(1L, "wrongold", "newpass"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void changePassword_valid_newPasswordHashed() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.changePassword(1L, "password123", "newpass");

        assertTrue(encoder.matches("newpass", testUser.getPassword()));
    }


    @Test
    public void changeBio_notLoggedIn_throwsUnauthorized() {
        when(userRepository.findByToken("bad")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.changeBio(1L, "new bio", "bad"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void changeBio_wrongUser_throwsForbidden() {
        User other = new User();
        other.setId(2L);
        other.setToken("valid-token");
        when(userRepository.findByToken("valid-token")).thenReturn(other);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.changeBio(1L, "new bio", "valid-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void changeBio_valid_bioUpdated() {
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.changeBio(1L, "I love hiking", "valid-token");

        assertEquals("I love hiking", testUser.getBio());
    }


    @Test
    public void deleteUser_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> userService.deleteUser(99L, "valid-token", "password123"));
    }

    @Test
    public void deleteUser_wrongToken_throwsUnauthorized() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.deleteUser(1L, "wrong-token", "password123"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void deleteUser_wrongPassword_throwsUnauthorized() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.deleteUser(1L, "valid-token", "wrongpass"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void deleteUser_valid_callsDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L, "valid-token", "password123");

        verify(userRepository, times(1)).delete(testUser);
    }


    @Test
    public void addUnavailability_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Unavailability u = new Unavailability();
        assertThrows(ResponseStatusException.class,
            () -> userService.addUnavailability(99L, u));
    }

    @Test
    public void addUnavailability_sourceSetToManual() {
        Unavailability u = new Unavailability();
        u.setStartDateTime(LocalDateTime.now());
        u.setEndDateTime(LocalDateTime.now().plusHours(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unavailabilityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.addUnavailability(1L, u);

        assertEquals("manual", u.getSource());
    }

    @Test
    public void addUnavailability_userLinked() {
        Unavailability u = new Unavailability();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(unavailabilityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.addUnavailability(1L, u);

        assertEquals(testUser, u.getUser());
    }


    @Test
    public void getUnavailabilities_returnsCorrectList() {
        Unavailability u1 = new Unavailability();
        Unavailability u2 = new Unavailability();
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(List.of(u1, u2));

        List<Unavailability> result = userService.getUnavailabilities(1L);
        assertEquals(2, result.size());
    }

    @Test
    public void getUnavailabilities_emptyList_returnsEmpty() {
        when(unavailabilityRepository.findByUserId(1L)).thenReturn(List.of());

        List<Unavailability> result = userService.getUnavailabilities(1L);
        assertTrue(result.isEmpty());
    }


    @Test
    public void deleteAllUnavailabilities_callsDeleteBySource() {
        userService.deleteAllUnavailabilities(1L);

        verify(unavailabilityRepository, times(1)).deleteByUserIdAndSource(1L, "manual");
    }


    @Test
    public void verifyToken_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> userService.verifyToken("token", 99L));
    }

    @Test
    public void verifyToken_validToken_noException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> userService.verifyToken("valid-token", 1L));
    }

    @Test
    public void verifyToken_invalidToken_throwsUnauthorized() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.verifyToken("wrong-token", 1L));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }


    @Test
    public void getUserById_found_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User found = userService.getUserById(1L);
        assertEquals(testUser, found);
    }

    @Test
    public void getUserById_notFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.getUserById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }


    @Test
    public void getUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<User> users = userService.getUsers();
        assertEquals(1, users.size());
    }
}