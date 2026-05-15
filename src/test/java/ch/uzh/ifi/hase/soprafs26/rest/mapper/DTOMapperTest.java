package ch.uzh.ifi.hase.soprafs26;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.RainPreference;
import ch.uzh.ifi.hase.soprafs26.constant.RoleType;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class DTOMapperTest {


    @Test
    public void timeWindow_morning_containsMorningHour() {
        assertTrue(TimeWindow.MORNING.isWithinWindow(LocalTime.of(8, 0)));
    }

    @Test
    public void timeWindow_morning_doesNotContainAfternoon() {
        assertFalse(TimeWindow.MORNING.isWithinWindow(LocalTime.of(13, 0)));
    }

    @Test
    public void timeWindow_afternoon_containsAfternoonHour() {
        assertTrue(TimeWindow.AFTERNOON.isWithinWindow(LocalTime.of(15, 0)));
    }

    @Test
    public void timeWindow_evening_containsEveningHour() {
        assertTrue(TimeWindow.EVENING.isWithinWindow(LocalTime.of(20, 0)));
    }

    @Test
    public void timeWindow_night_containsMidnight() {
        assertTrue(TimeWindow.NIGHT.isWithinWindow(LocalTime.of(23, 30)));
    }

    @Test
    public void timeWindow_night_containsEarlyMorning() {
        assertTrue(TimeWindow.NIGHT.isWithinWindow(LocalTime.of(2, 0)));
    }

    @Test
    public void timeWindow_night_doesNotContainMiddayHour() {
        assertFalse(TimeWindow.NIGHT.isWithinWindow(LocalTime.of(12, 0)));
    }

    @Test
    public void timeWindow_findWindow_morningReturnsCorrectWindow() {
        TimeWindow result = TimeWindow.findWindow(LocalTime.of(9, 0));
        assertEquals(TimeWindow.MORNING, result);
    }

    @Test
    public void timeWindow_findWindow_afternoonReturnsCorrectWindow() {
        assertEquals(TimeWindow.AFTERNOON, TimeWindow.findWindow(LocalTime.of(14, 0)));
    }

    @Test
    public void timeWindow_findWindow_eveningReturnsCorrectWindow() {
        assertEquals(TimeWindow.EVENING, TimeWindow.findWindow(LocalTime.of(19, 0)));
    }

    @Test
    public void timeWindow_findWindow_nightReturnsNight() {
        assertEquals(TimeWindow.NIGHT, TimeWindow.findWindow(LocalTime.of(23, 0)));
    }

    @Test
    public void timeWindow_morning_startsAt6() {
        assertEquals(LocalTime.of(6, 0), TimeWindow.MORNING.getStartTime());
    }

    @Test
    public void timeWindow_morning_endsAt12() {
        assertEquals(LocalTime.of(12, 0), TimeWindow.MORNING.getEndTime());
    }

    @Test
    public void timeWindow_custom_hasNullTimes() {
        assertNull(TimeWindow.CUSTOM.getStartTime());
        assertNull(TimeWindow.CUSTOM.getEndTime());
    }

    @Test
    public void activity_settersAndGetters_work() {
        Activity a = new Activity();
        a.setId(1L);
        a.setName("Run");
        a.setMinSize(2);
        a.setMaxSize(10);
        a.setDuration(3);
        a.setWeatherDependent(true);
        a.setMinTemp(-5);
        a.setMaxTemp(30);
        a.setRainPreference(RainPreference.NoRain);
        a.setLocation("Zurich");
        a.setRecursive(false);
        a.setStatus(ActivityStatus.PENDING);
        a.setTimePreference(TimeWindow.MORNING);
        a.setStartTime(LocalTime.of(6, 0));
        a.setEndTime(LocalTime.of(12, 0));
        a.setScheduledTime(LocalDateTime.of(2026, 6, 1, 9, 0));

        assertEquals(1L, a.getId());
        assertEquals("Run", a.getName());
        assertEquals(2, a.getMinSize());
        assertEquals(10, a.getMaxSize());
        assertEquals(3, a.getDuration());
        assertTrue(a.isWeatherDependent());
        assertEquals(-5, a.getMinTemp());
        assertEquals(30, a.getMaxTemp());
        assertEquals(RainPreference.NoRain, a.getRainPreference());
        assertEquals("Zurich", a.getLocation());
        assertFalse(a.isRecursive());
        assertEquals(ActivityStatus.PENDING, a.getStatus());
        assertEquals(TimeWindow.MORNING, a.getTimePreference());
        assertEquals(LocalTime.of(6, 0), a.getStartTime());
        assertEquals(LocalTime.of(12, 0), a.getEndTime());
        assertNotNull(a.getScheduledTime());
    }

    @Test
    public void group_settersAndGetters_work() {
        Group g = new Group();
        g.setGroupId(5L);
        g.setName("Friends");
        g.setJoinPassword("hashed");

        assertEquals(5L, g.getGroupId());
        assertEquals("Friends", g.getName());
        assertEquals("hashed", g.getJoinPassword());
    }

    @Test
    public void group_adminId_noMembers_returnsNull() {
        Group g = new Group();
        assertNull(g.getAdminId());
    }

    @Test
    public void group_adminId_withAdmin_returnsAdminId() {
        Group g = new Group();
        User admin = new User();
        admin.setId(42L);

        GroupMember gm = new GroupMember();
        gm.setUser(admin);
        gm.setGroup(g);
        gm.setRole(RoleType.ADMIN);

        g.getMembers().add(gm);

        assertEquals(42L, g.getAdminId());
    }

    @Test
    public void group_adminId_onlyMembers_returnsNull() {
        Group g = new Group();
        User u = new User();
        u.setId(1L);

        GroupMember gm = new GroupMember();
        gm.setUser(u);
        gm.setGroup(g);
        gm.setRole(RoleType.MEMBER);

        g.getMembers().add(gm);

        assertNull(g.getAdminId());
    }

	@Test
    public void groupMember_settersAndGetters_work() {
        GroupMember gm = new GroupMember();
        User u = new User();
        u.setId(1L);
        Group g = new Group();
        g.setGroupId(2L);

        gm.setMemberId(10L);
        gm.setUser(u);
        gm.setGroup(g);
        gm.setRole(RoleType.MEMBER);

        assertEquals(10L, gm.getMemberId());
        assertEquals(u, gm.getUser());
        assertEquals(g, gm.getGroup());
        assertEquals(RoleType.MEMBER, gm.getRole());
    }

    @Test
    public void unavailability_settersAndGetters_work() {
        Unavailability u = new Unavailability();
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 17, 0);

        u.setId(99L);
        u.setStartDateTime(start);
        u.setEndDateTime(end);
        u.setSource("manual");

        assertEquals(99L, u.getId());
        assertEquals(start, u.getStartDateTime());
        assertEquals(end, u.getEndDateTime());
        assertEquals("manual", u.getSource());
    }

    @Test
    public void activityVote_settersAndGetters_work() {
        ActivityVote vote = new ActivityVote();
        Activity a = new Activity();
        User u = new User();

        vote.setId(1L);
        vote.setActivity(a);
        vote.setUser(u);
        vote.setWantsToJoin(true);

        assertEquals(1L, vote.getId());
        assertEquals(a, vote.getActivity());
        assertEquals(u, vote.getUser());
        assertTrue(vote.isWantsToJoin());
    }

    @Test
    public void activityVote_wantsToJoinFalse_works() {
        ActivityVote vote = new ActivityVote();
        vote.setWantsToJoin(false);
        assertFalse(vote.isWantsToJoin());
    }

    @Test
    public void mapActivityPostDTOtoEntity_nameMapped() {
        ActivityPostDTO dto = new ActivityPostDTO();
        dto.setName("Picnic");
        dto.setMinSize(2);
        dto.setMaxSize(8);

        Activity a = DTOMapper.INSTANCE.convertActivityPostDTOtoEntity(dto);
        assertEquals("Picnic", a.getName());
    }

    @Test
    public void mapActivityPostDTOtoEntity_sizesMapped() {
        ActivityPostDTO dto = new ActivityPostDTO();
        dto.setName("X");
        dto.setMinSize(3);
        dto.setMaxSize(12);

        Activity a = DTOMapper.INSTANCE.convertActivityPostDTOtoEntity(dto);
        assertEquals(3, a.getMinSize());
        assertEquals(12, a.getMaxSize());
    }

    @Test
    public void mapActivityPostDTOtoEntity_statusIgnored() {
        ActivityPostDTO dto = new ActivityPostDTO();
        dto.setName("X");

        Activity a = DTOMapper.INSTANCE.convertActivityPostDTOtoEntity(dto);
        assertNull(a.getStatus());
    }

    @Test
    public void mapActivityPostDTOtoEntity_createdByIgnored() {
        ActivityPostDTO dto = new ActivityPostDTO();
        dto.setName("X");
        dto.setCreatedBy(99L);

        Activity a = DTOMapper.INSTANCE.convertActivityPostDTOtoEntity(dto);
        assertNull(a.getCreatedBy());
    }

    @Test
    public void mapActivityPostDTOtoEntity_groupIgnored() {
        ActivityPostDTO dto = new ActivityPostDTO();
        dto.setName("X");

        Activity a = DTOMapper.INSTANCE.convertActivityPostDTOtoEntity(dto);
        assertNull(a.getGroup());
    }

    @Test
    public void mapEntityToActivityGetDTO_idMapped() {
        Activity a = new Activity();
        a.setId(7L);
        a.setName("Run");

        ActivityGetDTO dto = DTOMapper.INSTANCE.convertEntityToActivityGetDTO(a);
        assertEquals(7L, dto.getId());
    }

    @Test
    public void mapEntityToActivityGetDTO_creatorIdMapped() {
        Activity a = new Activity();
        User creator = new User();
        creator.setId(3L);
        a.setCreatedBy(creator);

        ActivityGetDTO dto = DTOMapper.INSTANCE.convertEntityToActivityGetDTO(a);
        assertEquals(3L, dto.getCreatorId());
    }

    @Test
    public void mapEntityToActivityGetDTO_groupIdMapped() {
        Activity a = new Activity();
        Group g = new Group();
        g.setGroupId(55L);
        a.setGroup(g);

        ActivityGetDTO dto = DTOMapper.INSTANCE.convertEntityToActivityGetDTO(a);
        assertEquals(55L, dto.getGroupId());
    }

    @Test
    public void mapGroupPostDTOtoEntity_nameMapped() {
        GroupPostDTO dto = new GroupPostDTO();
        dto.setName("Hiking Crew");

        Group g = DTOMapper.INSTANCE.convertGroupPostDTOtoEntity(dto);
        assertEquals("Hiking Crew", g.getName());
    }

    @Test
    public void mapGroupPostDTOtoEntity_passwordMapped() {
        GroupPostDTO dto = new GroupPostDTO();
        dto.setName("X");
        dto.setJoinPassword("secret");

        Group g = DTOMapper.INSTANCE.convertGroupPostDTOtoEntity(dto);
        assertEquals("secret", g.getJoinPassword());
    }

    @Test
    public void mapGroupEntityToGetDTO_idMapped() {
        Group g = new Group();
        g.setGroupId(8L);
        g.setName("Cool Group");

        GroupGetDTO dto = DTOMapper.INSTANCE.convertEntityToGroupGetDTO(g);
        assertEquals(8L, dto.getId());
        assertEquals("Cool Group", dto.getName());
    }

    @Test
    public void mapUnavailabilityPostDTOtoEntity_datesMapped() {
        UnavailabilityPostDTO dto = new UnavailabilityPostDTO();
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 1, 17, 0);
        dto.setStartDateTime(start);
        dto.setEndDateTime(end);

        Unavailability u = DTOMapper.INSTANCE.convertUnavailabilityPostDTOtoEntity(dto);
        assertEquals(start, u.getStartDateTime());
        assertEquals(end, u.getEndDateTime());
    }

    @Test
    public void mapUnavailabilityEntityToGetDTO_idMapped() {
        Unavailability u = new Unavailability();
        u.setId(20L);
        u.setStartDateTime(LocalDateTime.now());
        u.setEndDateTime(LocalDateTime.now().plusHours(1));

        UnavailabilityGetDTO dto = DTOMapper.INSTANCE.convertEntityToUnavailabilityGetDTO(u);
        assertEquals(20L, dto.getId());
    }

    @Test
    public void calendarEventGetDTO_settersAndGetters_work() {
        CalendarEventGetDTO dto = new CalendarEventGetDTO();
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 10, 0);

        dto.setId(5L);
        dto.setStartDateTime(start);
        dto.setEndDateTime(end);
        dto.setSource("google");

        assertEquals(5L, dto.getId());
        assertEquals(start, dto.getStartDateTime());
        assertEquals(end, dto.getEndDateTime());
        assertEquals("google", dto.getSource());
    }

    @Test
    public void activityStatus_allValuesExist() {
        assertNotNull(ActivityStatus.PENDING);
        assertNotNull(ActivityStatus.VOTING);
        assertNotNull(ActivityStatus.SCHEDULED);
        assertNotNull(ActivityStatus.FAILED);
        assertNotNull(ActivityStatus.PAST);
    }

    @Test
    public void rainPreference_allValuesExist() {
        assertNotNull(RainPreference.NoRain);
        assertNotNull(RainPreference.Rain);
        assertNotNull(RainPreference.Any);
    }

    @Test
    public void googleCalendarToken_settersAndGetters_work() {
        GoogleCalendarToken token = new GoogleCalendarToken();
        User u = new User();
        LocalDateTime exp = LocalDateTime.now().plusHours(1);

        token.setId(1L);
        token.setUser(u);
        token.setAccessToken("access");
        token.setRefreshToken("refresh");
        token.setExpiresAt(exp);

        assertEquals(1L, token.getId());
        assertEquals(u, token.getUser());
        assertEquals("access", token.getAccessToken());
        assertEquals("refresh", token.getRefreshToken());
        assertEquals(exp, token.getExpiresAt());
    }

    @Test
    public void user_bioSetterGetter() {
        User u = new User();
        u.setBio("Loves hiking");
        assertEquals("Loves hiking", u.getBio());
    }

    @Test
    public void user_nullBio_returnsNull() {
        User u = new User();
        assertNull(u.getBio());
    }
}