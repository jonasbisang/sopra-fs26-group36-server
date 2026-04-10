package ch.uzh.ifi.hase.soprafs26.controller;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.JoinGroupDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public void leaveGroup(@PathVariable Long groupId, @PathVariable Long userId,  @RequestHeader("Authorization") String token) {
        groupService.leaveGroup(groupId, userId, token);
    }

}
