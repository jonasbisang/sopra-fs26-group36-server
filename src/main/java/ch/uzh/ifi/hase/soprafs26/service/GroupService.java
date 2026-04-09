package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.RoleType;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMemberRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public GroupService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    public Group createGroup(Group newGroup, String token) { //maybe add userID because frontend specified
        User creator = userRepository.findByToken(token);
        if (creator == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (newGroup.getName() == null || newGroup.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name cannot be empty");
        }
                if (groupRepository.existsByName(newGroup.getName())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group name is already taken");
        }
        if (newGroup.getJoinPassword() != null && !newGroup.getJoinPassword().isEmpty()) {
            newGroup.setJoinPassword(passwordEncoder.encode(newGroup.getJoinPassword()));
        }
        Group savedGroup = groupRepository.save(newGroup);
        groupRepository.flush();

        GroupMember admin = new GroupMember();
        admin.setUser(creator);
        admin.setGroup(savedGroup);
        admin.setRole(RoleType.ADMIN);
        admin.setJoinDate(LocalDate.now());
        groupMemberRepository.save(admin);
        groupMemberRepository.flush();

        return savedGroup;
    }
    public GroupMember joinGroup(Long groupId, String joinPassword, String token) {
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group doesn't exist"));

        if (groupMemberRepository.findByGroupAndUser(group, user) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already member enrolled");
        }
        if (group.getJoinPassword() != null ) {
            if (!passwordEncoder.matches(joinPassword, group.getJoinPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong group password");
            }
        }
        GroupMember newMember = new GroupMember();
        newMember.setUser(user);
        newMember.setGroup(group);
        newMember.setRole(RoleType.MEMBER); 
        newMember.setJoinDate(LocalDate.now());

        groupMemberRepository.save( newMember);
        groupMemberRepository.flush();
        return newMember;
    }
    
    public void leaveGroup(Long groupId, String token) {
        User user = userRepository.findByToken(token); 
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not logged in");
        }
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group doesn't exist"));
        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a member");
        }
        if (member.getRole() == RoleType.ADMIN) {
            List<GroupMember> admins = groupMemberRepository.findByGroupAndRole(group, RoleType.ADMIN);
            if (admins.size() <= 1 ) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are the only admin. Make another member admin before leaving.");
            }       
        }
        
        groupMemberRepository.delete(member);
        groupMemberRepository.flush();
    }
}
