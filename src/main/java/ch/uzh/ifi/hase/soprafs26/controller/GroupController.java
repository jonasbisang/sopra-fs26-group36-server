package ch.uzh.ifi.hase.soprafs26.controller;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupPasswordDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.JoinGroupDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class GroupController {
    private final GroupService groupService;
    private final GroupRepository groupRepository;

    GroupController(GroupRepository groupRepository, GroupService groupService) {
        this.groupRepository = groupRepository;
        this.groupService = groupService;
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GroupGetDTO createGroup(@RequestBody GroupPostDTO groupPostDTO,  @RequestHeader("Authorization") String token) {
        Group groupInput = DTOMapper.INSTANCE.convertGroupPostDTOtoEntity(groupPostDTO);
        Group createdGroup = groupService.createGroup(groupInput, token);
        return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(createdGroup);
    }

    @PostMapping("/groups/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GroupGetDTO joinGroup(@PathVariable Long groupId, @RequestBody(required = false) JoinGroupDTO joinGroupDTO,  @RequestHeader("Authorization") String token) {
        String joinPassword = null;
        if (joinGroupDTO != null) {
            joinPassword = joinGroupDTO.getJoinPassword();
        } 
        groupService.joinGroup(groupId, joinPassword, token);
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
    }

    @DeleteMapping("/groups/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) 
    public void removeMember(@PathVariable Long groupId, @PathVariable Long userId,  @RequestHeader("Authorization") String token) {
        groupService.removeMember(groupId, userId, token);
    }

    @PutMapping("/groups/{groupId}/members/{userId}/role") // no RoleUpdateDTO implemented (not needed for now)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promoteMember(@PathVariable Long groupId, @PathVariable Long userId, @RequestHeader("Authorization") String token) {
        groupService.promoteMember(groupId, userId, token);
    }

    @PutMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeGroupPassword(@PathVariable Long groupId, @RequestBody GroupPasswordDTO groupPasswordDTO, @RequestHeader("Authorization") String token) {
        groupService.changeGroupPassword(groupId, groupPasswordDTO.getOldPassword(), groupPasswordDTO.getNewPassword(), token);
    }

    @DeleteMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long groupId, @RequestHeader("Authorization") String token) {
        groupService.deleteGroup(groupId, token);
    }

    @GetMapping("/groups/{groupId}/members")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getGroupMembers(@PathVariable Long groupId,
                                        @RequestHeader("Authorization") String token) {
    List<User> members = groupService.getGroupMembers(groupId, token);
    return members.stream()
            .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
            .collect(Collectors.toList());
    }
}
