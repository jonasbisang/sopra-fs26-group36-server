package ch.uzh.ifi.hase.soprafs26.repository;
import ch.uzh.ifi.hase.soprafs26.constant.RoleType;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    GroupMember findByGroupAndUser (Group group, User user);
    GroupMember findyByGroupAndRole(Group group, RoleType role);
}
