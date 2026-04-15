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
        if (groupRepository.existsByName(newGroup.getName())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group name is already taken");
        }
        if (newGroup.getName() == null || newGroup.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name cannot be empty");
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

}
