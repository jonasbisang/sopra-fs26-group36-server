package ch.uzh.ifi.hase.soprafs26.repository;
import ch.uzh.ifi.hase.soprafs26.constant.RoleType;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    GroupMember findByGroupAndUser(Group group, User user);
    List<GroupMember> findByGroupAndRole(Group group, RoleType role);
}
