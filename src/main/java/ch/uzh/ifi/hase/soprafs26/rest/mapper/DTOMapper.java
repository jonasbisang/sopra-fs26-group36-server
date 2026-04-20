package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupPostDTO;import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UnavailabilityGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UnavailabilityPostDTO;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(target = "id", ignore = true)
    @Mapping(target = "token", ignore = true)
    @Mapping(source = "name", target = "name")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
	@Mapping(source = "email", target = "email")
    @Mapping(source = "bio", target = "bio")
    @Mapping(target = "creationDate", ignore = true)
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "email", target = "email")   
	@Mapping(source = "bio", target = "bio")
	UserGetDTO convertEntityToUserGetDTO(User user);

	@Mapping(source = "startDateTime", target = "startDateTime")
	@Mapping(source = "endDateTime", target = "endDateTime")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	Unavailability convertUnavailabilityPostDTOtoEntity(UnavailabilityPostDTO unavailabilityPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "startDateTime", target = "startDateTime")
	@Mapping(source = "endDateTime", target = "endDateTime")
	UnavailabilityGetDTO convertEntityToUnavailabilityGetDTO(Unavailability unavailability);


	//convertUserPutDTOtoEntity is missing

	@Mapping(target = "groupId", ignore = true)
	@Mapping(source = "name", target = "name")
	@Mapping(source = "joinPassword", target = "joinPassword")
	@Mapping(target = "members", ignore = true)
	Group convertGroupPostDTOtoEntity(GroupPostDTO groupPostDTO);

	@Mapping(source = "name", target = "name")
	@Mapping(source = "groupId", target = "groupId")
	GroupGetDTO convertEntityToGroupGetDTO(Group group);
}


