package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.RoleType;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMemberRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.ArrayList;
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

    public Group createGroup(Group newGroup, String token) { 
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
    public void removeMember(Long groupId,  Long userId, String token) {
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not logged in");
        }
        if (user.getId().equals(userId)) {
            leaveGroup(groupId, userId, token);
        } else {
            kickMember(groupId, userId, token);
        }
    }

    public void leaveGroup(Long groupId,  Long userId, String token) { // userID is unneccessary but its in spec 
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

    public void promoteMember(Long groupId, Long memberId, String adminToken) {
        User admin = userRepository.findByToken(adminToken);
        if (admin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin isn't logged in");
        }
        User member = userRepository.findById(memberId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User to be promoted not doesn't exist"));
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This group does not exist."));
        GroupMember groupAdmin = groupMemberRepository.findByGroupAndUser(group, admin);
        GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, member);
        if (groupAdmin == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user isn't a member of the group");
        }
        if (groupMember == null ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The user to be promoted isn't part of the group");
        }
        if (groupAdmin.getRole() != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user is not an admin. Only an admin can promote others");
        }
        groupMember.setRole(RoleType.ADMIN);
        groupMemberRepository.save(groupMember);
        groupMemberRepository.flush();
    }

    public void kickMember(Long groupId, Long memberId, String adminToken) {
        User admin = userRepository.findByToken(adminToken);
        if (admin == null ){ throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in"); }
        User member = userRepository.findById(memberId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User to be kicked doesn't exist"));
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "group doesn't exist"));
        GroupMember groupAdmin = groupMemberRepository.findByGroupAndUser(group, admin);
        if (groupAdmin == null || groupAdmin.getRole() != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only admins can kick members");
        }
        GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, member);
        if (groupMember == null ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User to be kicked is not part of the group");
        }
        if ( groupMember.getRole() != RoleType.MEMBER){ 
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot be kicked"); 
        }
        groupMemberRepository.delete(groupMember);
        groupMemberRepository.flush();
    }

    public void deleteGroup(Long groupId, String token) {
        User admin = userRepository.findByToken(token);
        if (admin == null ) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Group group = groupRepository.findById(groupId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group doesn't exist"));
        GroupMember groupAdmin = groupMemberRepository.findByGroupAndUser(group, admin);
        if (groupAdmin == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not part of the group");
        }
        if (groupAdmin.getRole() != RoleType.ADMIN){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only admins can delete group.");
        }
        groupRepository.delete(group);
        groupRepository.flush();
    }
    public void changeGroupPassword(Long groupId, String oldPassword, String newPassword, String token) {
        User admin = userRepository.findByToken(token);
        if (admin == null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"); }
        Group group = groupRepository.findById(groupId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group doesn't exist")); 
        GroupMember groupAdmin = groupMemberRepository.findByGroupAndUser(group, admin);
        if (groupAdmin == null || groupAdmin.getRole() != RoleType.ADMIN) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can change group password"); }
        if (!passwordEncoder.matches(oldPassword, group.getJoinPassword())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Old password is incorrect");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change to an empty password");
        }
        group.setJoinPassword(passwordEncoder.encode(newPassword));
        groupRepository.save(group);
        groupRepository.flush();
    }

    public List<Group> getGroupsByUser(Long userId, String token) { //might want to add constraint that you can only see the groups of users in the same group as you
        User user = userRepository.findByToken(token); 
        if (user == null ) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        User targetUser = userRepository.findById(userId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "The searched user not found."));
        List<GroupMember> memberships = groupMemberRepository.findByUser(targetUser);
        List<Group> groups = new ArrayList<>();
        for (GroupMember membership : memberships) {
            groups.add(membership.getGroup());
        }
        return groups;
    }
}
