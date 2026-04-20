package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

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

}
