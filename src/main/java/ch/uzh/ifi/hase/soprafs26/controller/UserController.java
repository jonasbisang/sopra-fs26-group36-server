package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UnavailabilityGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UnavailabilityPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMemberRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupGetDTO;

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
		private final GroupMemberRepository groupMemberRepository;

			UserController(UserService userService, GroupMemberRepository groupMemberRepository) {
    				this.userService = userService;
    				this.groupMemberRepository = groupMemberRepository;
			}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO loginUser (@RequestBody UserPostDTO userPostDTO) {
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
		User loggedInUser = userService.loginUser(userInput);
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loggedInUser);
	}
	@PostMapping("/users/logout")
	@ResponseStatus(HttpStatus.OK)
	public void logoutUser(@RequestHeader("Authorization") String token) {
		userService.logoutUser(token);
	}

	@GetMapping("/users")
	@ResponseStatus(HttpStatus.OK)
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



	@PutMapping("/users/{id}/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@PathVariable Long id, @RequestBody UserPutDTO userPutDTO) {
    userService.changePassword(id, userPutDTO.getOldPassword(), userPutDTO.getNewPassword());
		}

	@PutMapping("/users/{id}/username")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changeUsername(
        @PathVariable Long id,
        @RequestHeader("Authorization") String token,
        @RequestBody UserPutDTO userPutDTO) {
    	userService.changeUsername(id, token, userPutDTO.getNewUsername());
}

	@DeleteMapping("/users/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(
        @PathVariable Long id,
        @RequestHeader("Authorization") String token,
        @RequestBody UserPutDTO userPutDTO) {
    	userService.deleteUser(id, token, userPutDTO.getOldPassword());
}

	@PostMapping("/users/{Id}/unavailability")
	public ResponseEntity<UnavailabilityGetDTO> addUnavailability(
			@PathVariable Long Id,
			@RequestHeader("Authorization") String token,
			@RequestBody UnavailabilityPostDTO unavailabilityPostDTO) {

		userService.verifyToken(token, Id);
		Unavailability unavailability = DTOMapper.INSTANCE.convertUnavailabilityPostDTOtoEntity(unavailabilityPostDTO);
		Unavailability saved = userService.addUnavailability(Id, unavailability);
		return ResponseEntity.status(HttpStatus.CREATED).body(DTOMapper.INSTANCE.convertEntityToUnavailabilityGetDTO(saved));
}

	@GetMapping("/users/{id}/unavailability")
	@ResponseStatus(HttpStatus.OK)
	public List<UnavailabilityGetDTO> getUnavailabilities(
        @PathVariable Long id,
        @RequestHeader("Authorization") String token) {
    	userService.verifyToken(token, id);
    	List<Unavailability> unavailabilities = userService.getUnavailabilities(id);
    	List<UnavailabilityGetDTO> result = new ArrayList<>();
    	for (Unavailability u : unavailabilities) {
    	    result.add(DTOMapper.INSTANCE.convertEntityToUnavailabilityGetDTO(u));
    	}
    	return result;
	}

	@DeleteMapping("/users/{id}/unavailability")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAllUnavailabilities(
        @PathVariable Long id,
        @RequestHeader("Authorization") String token) {
    	userService.verifyToken(token, id);
    	userService.deleteAllUnavailabilities(id);
}

	@GetMapping("/users/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO getUserById(@PathVariable Long id) {
    	User user = userService.getUserById(id);
    	return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
}	


	@GetMapping("/users/{id}/groups")
	@ResponseStatus(HttpStatus.OK)
	public List<GroupGetDTO> getGroupsForUser(
    	@PathVariable Long id,
    	@RequestHeader("Authorization") String token) {
    
    List<GroupMember> memberships = groupMemberRepository.findByUserId(id);
    List<GroupGetDTO> result = new ArrayList<>();
    for (GroupMember gm : memberships) {
        GroupGetDTO dto = DTOMapper.INSTANCE.convertEntityToGroupGetDTO(gm.getGroup());
        dto.setMembers(groupMemberRepository.findByGroup(gm.getGroup()).size());
        result.add(dto);
    }
    return result;
}
}


