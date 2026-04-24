package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import ch.uzh.ifi.hase.soprafs26.repository.UnavailabilityRepository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UnavailabilityRepository unavailabilityRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setId(1L);
		testUser.setName("testName");
		testUser.setUsername("testUsername");
		testUser.setPassword("testPassword");
		testUser.setEmail("testEmail");
		testUser.setStatus(UserStatus.OFFLINE);
		testUser.setToken("testToken");

		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
	}

	@Test
	public void createUser_validInputs_success() {
		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		User createdUser = userService.createUser(testUser);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

		assertEquals(testUser.getId(), createdUser.getId());
		assertEquals(testUser.getName(), createdUser.getName());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertEquals(testUser.getPassword(), createdUser.getPassword());
		assertEquals(testUser.getEmail(), createdUser.getEmail());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
	}

	@Test
	public void createUser_duplicateUsername_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByEmail(Mockito.any())).thenReturn(null);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void createUser_duplicateEmail_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByEmail(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void createUser_emptyPassword_throwsException() {

		testUser.setPassword("");

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
}
	@Test
	public void addUnavailability_validInputs_success() {
		Unavailability unavailability = new Unavailability();
		unavailability.setStartDateTime(LocalDateTime.of(2026, 5, 1, 9, 0));
		unavailability.setEndDateTime(LocalDateTime.of(2026, 5, 1, 17, 0));

		Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));
		Mockito.when(unavailabilityRepository.save(Mockito.any())).thenReturn(unavailability);

		Unavailability result = userService.addUnavailability(1L, unavailability);

		Mockito.verify(unavailabilityRepository, Mockito.times(1)).save(Mockito.any());
		assertEquals(testUser, unavailability.getUser());
		assertEquals(unavailability.getStartDateTime(), result.getStartDateTime());
		assertEquals(unavailability.getEndDateTime(), result.getEndDateTime());
	}

	@Test
	public void addUnavailability_userNotFound_throwsException() {
		Unavailability unavailability = new Unavailability();
		unavailability.setStartDateTime(LocalDateTime.of(2026, 5, 1, 9, 0));
		unavailability.setEndDateTime(LocalDateTime.of(2026, 5, 1, 17, 0));

		Mockito.when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

		assertThrows(ResponseStatusException.class, () -> userService.addUnavailability(99L, unavailability));
	}

	@Test
	public void createUser_passwordIsHashed() {
    	User newUser = new User();
    	newUser.setUsername("testuser");
    	newUser.setPassword("mypassword");
    	newUser.setEmail("test@test.com");

    	Mockito.when(userRepository.findByUsername("testuser")).thenReturn(null);
    	Mockito.when(userRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

    	User created = userService.createUser(newUser);

    	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    	assertNotEquals("mypassword", created.getPassword());
    	assertTrue(encoder.matches("mypassword", created.getPassword()));
	}

	@Test
	public void loginUser_validCredentials_returnsToken() {
    	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    	User existing = new User();
    	existing.setUsername("testuser");
    	existing.setPassword(encoder.encode("mypassword"));
    	existing.setStatus(UserStatus.OFFLINE);

    	Mockito.when(userRepository.findByUsername("testuser")).thenReturn(existing);
    	Mockito.when(userRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

    	User input = new User();
    	input.setUsername("testuser");
    	input.setPassword("mypassword");

    	User loggedIn = userService.loginUser(input);

    	assertNotNull(loggedIn.getToken());
    	assertEquals(UserStatus.ONLINE, loggedIn.getStatus());
	}

	@Test
	public void logoutUser_clearsToken() {
    	User user = new User();
    	user.setToken("valid-token");
    	user.setStatus(UserStatus.ONLINE);

    	Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
    	Mockito.when(userRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

    	userService.logoutUser("valid-token");

    	assertNull(user.getToken());
    	assertEquals(UserStatus.OFFLINE, user.getStatus());
	}

	@Test
	public void changeUsername_success() {
    	Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));
    	Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(null);

    	userService.changeUsername(1L, "testToken", "newUsername");
		System.out.println("changeUsername_success - new username: " + testUser.getUsername());
    	assertEquals("newUsername", testUser.getUsername());
	}

	@Test
	public void changeUsernameDuplicateError() {
    	User otherUser = new User();
    	otherUser.setUsername("newUsername");

    	Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));
    	Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(otherUser);

    	assertThrows(ResponseStatusException.class, () ->
        userService.changeUsername(1L, "testToken", "newUsername"));	
	}	

	@Test
	public void changePassword_wrongOldPassword_error() {
    	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    	testUser.setPassword(encoder.encode("correctPassword"));

    	Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));
		assertThrows(ResponseStatusException.class, () ->
        userService.changePassword(1L, "wrongPassword", "newPassword"));
	}


	@Test
	public void changePassword_success() {
    	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    	testUser.setPassword(encoder.encode("oldPassword"));

    	Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

    	userService.changePassword(1L, "oldPassword", "newPassword");

    	assertTrue(encoder.matches("newPassword", testUser.getPassword()));
}

	@Test
	public void deleteUser_validCredentials_success() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		testUser.setPassword(encoder.encode("testPassword"));
		testUser.setToken("valid-token");

		Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

		userService.deleteUser(1L, "valid-token", "testPassword");

		Mockito.verify(userRepository, Mockito.times(1)).delete(testUser);
}
	@Test
	public void deleteUser_wrongPassword_throwsUnauthorized() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		testUser.setPassword(encoder.encode("correctPassword"));
		testUser.setToken("valid-token");

		Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

		assertThrows(ResponseStatusException.class, () -> 
			userService.deleteUser(1L, "valid-token", "wrongPassword"));
	}

	@Test
	public void verifyToken_invalidToken_throwsUnauthorized() {
		testUser.setToken("actual-token");
		Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
			userService.verifyToken("fake-token", 1L));
		
		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
}

	@Test
	public void deleteAllUnavailabilities_success() {
		Unavailability u1 = new Unavailability();
		Unavailability u2 = new Unavailability();
		List<Unavailability> list = List.of(u1, u2);

		Mockito.when(unavailabilityRepository.findByUserId(1L)).thenReturn(list);

		userService.deleteAllUnavailabilities(1L);

		Mockito.verify(unavailabilityRepository, Mockito.times(1)).deleteAll(list);
}

	@Test
	public void changeUsername_invalidToken_throwsUnauthorized() {
		testUser.setToken("secret-token");
		Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

		assertThrows(ResponseStatusException.class, () -> 
			userService.changeUsername(1L, "wrong-token", "newUsername"));
}

}
