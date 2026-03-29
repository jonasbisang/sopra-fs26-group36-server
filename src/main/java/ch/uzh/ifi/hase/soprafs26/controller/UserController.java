package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO loginUser (@RequestBody UserPostDTO userPostDTO) {
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
		User loggedInUser = userService.loginUser(userInput);
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loggedInUser);
	}
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.OK)
	public void logoutUser(@RequestHeader("Authorization") String token) {
		userService.logoutUser(token);
	}

	@GetMapping("/users")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ResponseBody
	public List<UserGetDTO> getAllUsers() {
		List<User> users = userService.getUsers();
		List<UserGetDTO> userGetDTOs = new ArrayList<>();

		for (User user : users) {
			userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
		}
		return userGetDTOs;
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		User createdUser = userService.createUser(userInput);
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
	}
}
