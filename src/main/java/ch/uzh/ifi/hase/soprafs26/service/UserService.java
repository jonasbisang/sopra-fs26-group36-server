package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import ch.uzh.ifi.hase.soprafs26.repository.UnavailabilityRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final UnavailabilityRepository unavailabilityRepository;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public UserService(@Qualifier("userRepository") UserRepository userRepository,
                   UnavailabilityRepository unavailabilityRepository) {
    this.userRepository = userRepository;
    this.unavailabilityRepository = unavailabilityRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User createUser(User newUser) {
		
		if (newUser.getUsername() == null || newUser.getUsername().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must not be empty.");
		}
		if (newUser.getPassword() == null || newUser.getPassword().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not be empty.");
		}
		if (newUser.getEmail() == null || newUser.getEmail().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be empty.");
		}


		newUser.setToken(UUID.randomUUID().toString());
		newUser.setCreationDate(LocalDate.now());
		newUser.setStatus(UserStatus.OFFLINE);

		String hashedPassword = passwordEncoder.encode(newUser.getPassword());
		newUser.setPassword(hashedPassword);

		checkIfUserExists(newUser);

		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	public User loginUser(User userInput) {
		User existingUser = userRepository.findByUsername(userInput.getUsername());
		if (existingUser == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username not found");
		}
		if (!passwordEncoder.matches(userInput.getPassword(), existingUser.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");
		}
		existingUser.setToken(UUID.randomUUID().toString());
		existingUser.setStatus(UserStatus.ONLINE);
		userRepository.save(existingUser);
		userRepository.flush();
		return existingUser;
	}

	public void logoutUser(String token) {
		User user = userRepository.findByToken(token);

		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
		}
		user.setStatus(UserStatus.OFFLINE);
		user.setToken(null);
		userRepository.save(user);
		userRepository.flush();
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		User userByEmail = userRepository.findByEmail(userToBeCreated.getEmail());

		String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
		if (userByUsername != null && userByEmail != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format(baseErrorMessage, "username and the name", "are"));
		} else if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
		} else if (userByEmail != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "Email", "is"));
		}
	}


	public void changePassword(Long id, String oldPassword, String newPassword) {
    User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (!user.getPassword().equals(oldPassword)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Old password is incorrect");
    }
    user.setPassword(newPassword);
    userRepository.save(user);
	}

	public void changeUsername(Long id, String token, String newUsername) {
    User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (!user.getToken().equals(token)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }

    if (userRepository.findByUsername(newUsername) != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken!");
    }
    user.setUsername(newUsername);
    userRepository.save(user);
	}

	public void deleteUser(Long id, String token, String password) {
    User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (!token.equals(user.getToken())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized for this action");
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");
    }

    userRepository.delete(user);
    userRepository.flush();
	}

	public Unavailability addUnavailability(Long userId, Unavailability unavailability) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    unavailability.setUser(user);
    return unavailabilityRepository.save(unavailability);
	}

}
