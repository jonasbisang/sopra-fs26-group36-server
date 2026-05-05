package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.ActivityStatus;
import ch.uzh.ifi.hase.soprafs26.constant.RainPreference;
import ch.uzh.ifi.hase.soprafs26.constant.TimeWindow;
import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.ActivityVote;
import ch.uzh.ifi.hase.soprafs26.entity.Unavailability;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.GroupMember;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UnavailabilityRepository;
import ch.uzh.ifi.hase.soprafs26.repository.ActivityVoteRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GroupMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.Objects;
import java.util.List;

@Service
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityVoteRepository activityVoteRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UnavailabilityRepository unavailabilityRepository;


    @Autowired
    public ActivityService(@Qualifier("activityRepository") ActivityRepository activityRepository,
                       @Qualifier("userRepository") UserRepository userRepository,
                       ActivityVoteRepository activityVoteRepository,
                       GroupRepository groupRepository, 
                       GroupMemberRepository groupMemberRepository,
                       UnavailabilityRepository unavailabilityRepository) {


    this.activityRepository = activityRepository;
    this.userRepository = userRepository;
    this.activityVoteRepository = activityVoteRepository;
    this.groupRepository = groupRepository;
    this.groupMemberRepository = groupMemberRepository;
    this.unavailabilityRepository = unavailabilityRepository;

}


    public Activity createActivity(Activity newActivity, Long createdBy, Long targetGroupId) {
    
    validateActivityInputs(newActivity);
    
    Group group = groupRepository.findById(targetGroupId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

    newActivity.setGroup(group);
    User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + createdBy));
        
        newActivity.setCreatedBy(creator);
        newActivity.setStatus(ActivityStatus.PENDING);
    
        return activityRepository.save(newActivity);
    }

    private void validateActivityInputs(Activity activity) {
        if (activity.getName() == null || activity.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity name cannot be empty!");
        }

        if (activity.getMinSize() > activity.getMaxSize()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum size cannot be greater than maximum size!");
            }
        if (activity.getMinSize() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum size must be at least 1!");
        }

        if (activity.getStartTime() != null && activity.getEndTime() != null && activity.getStartTime().isAfter(activity.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must be before end time!");
        }
    }


    public List<Activity> getActivities(Long groupId) {
    groupRepository.findById(groupId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    return activityRepository.findByGroupGroupIdAndStatus(groupId, ActivityStatus.SCHEDULED);
    }

    public void vote(Long groupId, Long activityId, boolean wantsToJoin, Long userId) {
    Activity activity = activityRepository.findById(activityId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

    if (!activity.getGroup().getGroupId().equals(groupId)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity does not belong to this group");}

    if (activityVoteRepository.existsByActivityIdAndUserId(activityId, userId)
        && activity.getStatus() != ActivityStatus.SCHEDULED) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already voted on this activity");}

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    long delayMs = 100 + (long)(Math.random() * 1400);
    try {
        Thread.sleep(delayMs);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    if (activity.getStatus() == ActivityStatus.SCHEDULED) {
        long currentParticipants = activityVoteRepository.countByActivityIdAndWantsToJoinTrue(activityId);
        if (currentParticipants >= activity.getMaxSize()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No slots available for this activity");
        }
    }

    ActivityVote vote = new ActivityVote();
    vote.setActivity(activity);
    vote.setUser(user);
    vote.setWantsToJoin(wantsToJoin);
    activityVoteRepository.save(vote);
    if (wantsToJoin) {
        long acceptCount = activityVoteRepository.countByActivityIdAndWantsToJoinTrue(activityId);
    if (acceptCount >= activity.getMinSize()) {
        findSchedule(activity);
            }
        }
    }

    private void parseTimeConditions(Activity activity) {
        TimeWindow preference = activity.getTimePreference();
    
        if (preference == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time preference must be specified.");
        }
    
        if (preference != TimeWindow.CUSTOM) {
            activity.setStartTime(preference.getStartTime());
            activity.setEndTime(preference.getEndTime());
        } 
        else {
            if (activity.getStartTime() == null || activity.getEndTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Custom time slots require both a start and end time.");
            }
            
            if (!activity.getStartTime().isBefore(activity.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Start time must be before end time for custom slots.");
            }
        }
    }

    public List<Activity> getProposedActivitiesByGroupId(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found with ID: " + groupId));
    
        List<Activity> activities = activityRepository.findByGroupGroupIdAndStatus(groupId, ActivityStatus.PENDING);
    
        return activities;
    }

    private void findSchedule(Activity activity) {
    // Fetching all user who want to participate
    List<User> participants = activityVoteRepository.findByActivityId(activity.getId())
        .stream()
        .filter(ActivityVote::isWantsToJoin)
        .map(ActivityVote::getUser)
        .collect(Collectors.toList());

    // get their unavailabilities (Only manual calendar for now, we will add google calendar next sprint!!)
    List<Unavailability> unavailabilities = participants.stream()
        .flatMap(u -> unavailabilityRepository.findByUserId(u.getId()).stream())
        .collect(Collectors.toList());

    LocalTime windowStart = activity.getStartTime() != null ? activity.getStartTime() : LocalTime.of(8, 0);
    LocalTime windowEnd = activity.getEndTime() != null ? activity.getEndTime() : LocalTime.of(22, 0);
    int duration = activity.getDuration() > 0 ? activity.getDuration() : 1;

    // Trying to find a slot in the next 14 days, otherwise it should be pushed to Vote again!!! --> Does this work already? Please check martha
    for (int day = 0; day < 14; day++) {
        LocalDate date = LocalDate.now().plusDays(day);
        if (activity.isWeatherDependent()) {
            boolean weatherCheck = checkWeather(date, activity);
            if (!weatherCheck) {
                continue;
            }
        }
        LocalDateTime candidate = LocalDateTime.of(date, windowStart);
        LocalDateTime latestStart = LocalDateTime.of(date, windowEnd).minusHours(duration);

        while (!candidate.isAfter(latestStart)) {
            final LocalDateTime start = candidate;
            final LocalDateTime end = candidate.plusHours(duration);

            // check if anyone is unavailable at this time
            boolean conflict = unavailabilities.stream().anyMatch(u ->
                start.isBefore(u.getEndDateTime()) && end.isAfter(u.getStartDateTime())
            );

        

            if (!conflict) {

                activity.setScheduledTime(start);
                activity.setStatus(ActivityStatus.SCHEDULED);
                activityRepository.save(activity);


                for (User p : participants) {
                    Unavailability block = new Unavailability();
                    block.setUser(p);
                    block.setStartDateTime(start);
                    block.setEndDateTime(end);
                    unavailabilityRepository.save(block);
                }
                return;
            }
            candidate = candidate.plusMinutes(30);
            }
        }
        activityVoteRepository.deleteByActivityId(activity.getId());
        activityRepository.delete(activity);
            }

    private boolean checkWeather(LocalDate date, Activity activity) {
        if (activity.getLocation() == null || activity.getLocation().trim().isEmpty()) {
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + activity.getLocation() + "&count=1&format=json";
            Map geoResponse = restTemplate.getForObject(geoUrl, Map.class);

            if (geoResponse == null || !geoResponse.containsKey("results")) {
                System.err.println("Location not found by Geocoder: " + activity.getLocation());
                return false;
            }
            List<Map<String, Object>> results = (List<Map<String, Object>>) geoResponse.get("results");
            double lat = ((Number) results.get(0).get("latitude")).doubleValue();
            double lon = ((Number) results.get(0).get("longitude")).doubleValue();

            String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + 
                                "&longitude=" + lon + 
                                "&start_date=" + date.toString() + 
                                "&end_date=" + date.toString() + 
                                "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum&timezone=auto";

            Map weatherResponse = restTemplate.getForObject(weatherUrl, Map.class);

            Map<String, List<Number>> daily = (Map<String, List<Number>>) weatherResponse.get("daily");

            double forecastMaxTemp = daily.get("temperature_2m_max").get(0).doubleValue();
            double forecastMinTemp = daily.get("temperature_2m_min").get(0).doubleValue();
            double precipitation = daily.get("precipitation_sum").get(0).doubleValue();
            boolean isRaining = precipitation > 0.0;
            if (forecastMaxTemp > activity.getMaxTemp() || forecastMinTemp < activity.getMinTemp()){
                return false;
            }
            RainPreference rainPref = activity.getRainPreference();
            if (rainPref != null && rainPref != RainPreference.Any) {
                if (rainPref == RainPreference.NoRain && isRaining) {
                    return false; 
                }
                if (rainPref == RainPreference.Rain && !isRaining) {
                    return false; 
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Weather API Error for " + activity.getLocation() + ": " + e.getMessage());
            return false;
        }
    }
    public List<Activity> getGroupCalendar(Long groupId, String token) {
        User requester = userRepository.findByToken(token);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        GroupMember membership = groupMemberRepository.findByGroupAndUser(group, requester);
        if (membership == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        return activityRepository.findByGroupGroupIdAndStatus(groupId, ActivityStatus.SCHEDULED);
    }
    public Activity reviveActivity(Long groupId, Long activityId, String token) {

        User requester = userRepository.findByToken(token);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        Activity original = activityRepository.findById(activityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        if (!original.getGroup().getGroupId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity does not belong to this group");
        }

        GroupMember membership = groupMemberRepository.findByGroupAndUser(original.getGroup(), requester);
        if (membership == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        Activity revived = new Activity();
        revived.setName(original.getName());
        revived.setMinSize(original.getMinSize());
        revived.setMaxSize(original.getMaxSize());
        revived.setDuration(original.getDuration());
        revived.setTimePreference(original.getTimePreference());
        revived.setStartTime(original.getStartTime());
        revived.setEndTime(original.getEndTime());
        revived.setWeatherDependent(original.isWeatherDependent());
        revived.setMinTemp(original.getMinTemp());
        revived.setMaxTemp(original.getMaxTemp());
        revived.setRainPreference(original.getRainPreference());
        revived.setLocation(original.getLocation());
        revived.setRecursive(original.isRecursive());
        revived.setGroup(original.getGroup());
        revived.setCreatedBy(requester);
        revived.setStatus(ActivityStatus.PENDING); 


        Activity savedRevived = activityRepository.save(revived);


        List<ActivityVote> oldVotes = activityVoteRepository.findByActivityId(activityId);
        activityVoteRepository.deleteAll(oldVotes);

        activityRepository.delete(original);
        activityRepository.flush();

        return savedRevived;
    }
}
