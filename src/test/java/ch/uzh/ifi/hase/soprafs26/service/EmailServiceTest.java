package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.RainPreference;
import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private Activity testActivity;
    private User userAlice;
    private User userBob;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        userAlice = new User();
        userAlice.setId(1L);
        userAlice.setUsername("alice");
        userAlice.setEmail("alice@friendler.com");

        userBob = new User();
        userBob.setId(2L);
        userBob.setUsername("bob");
        userBob.setEmail("bob@friendler.com");

        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setName("Hiking Trip");
        testActivity.setLocation("Zurich");
        testActivity.setDuration(3);
        testActivity.setScheduledTime(LocalDateTime.of(2026, 6, 15, 10, 0));
        testActivity.setWeatherDependent(false);
    }


    @Test
    public void sendEmail_validInput_sendsMessage() {
        emailService.sendEmail("test@test.com", "Hello", "World");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("test@test.com", sent.getTo()[0]);
        assertEquals("Hello", sent.getSubject());
        assertEquals("World", sent.getText());
    }

    @Test
    public void sendEmail_callsMailSenderExactlyOnce() {
        emailService.sendEmail("a@b.com", "subj", "body");
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }


    @Test
    public void sendActivityScheduledEmail_twoParticipants_sendsTwoEmails() {
        List<User> participants = Arrays.asList(userAlice, userBob);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    public void sendActivityScheduledEmail_subjectContainsActivityName() {
        List<User> participants = Collections.singletonList(userAlice);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertTrue(captor.getValue().getSubject().contains("Hiking Trip"));
    }

    @Test
    public void sendActivityScheduledEmail_bodyContainsFormattedDate() {
        List<User> participants = Collections.singletonList(userAlice);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertTrue(captor.getValue().getText().contains("15.06.2026 10:00"));
    }

    @Test
    public void sendActivityScheduledEmail_bodyContainsDuration() {
        List<User> participants = Collections.singletonList(userAlice);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertTrue(captor.getValue().getText().contains("3"));
    }

    @Test
    public void sendActivityScheduledEmail_bodyContainsParticipantUsername() {
        List<User> participants = Collections.singletonList(userAlice);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertTrue(captor.getValue().getText().contains("alice"));
    }

    @Test
    public void sendActivityScheduledEmail_userWithNoEmail_skipped() {
        User noEmail = new User();
        noEmail.setId(3L);
        noEmail.setUsername("ghost");
        noEmail.setEmail(null);

        emailService.sendActivityScheduledEmail(testActivity, Collections.singletonList(noEmail));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    public void sendActivityScheduledEmail_userWithBlankEmail_skipped() {
        User blankEmail = new User();
        blankEmail.setId(4L);
        blankEmail.setUsername("blank");
        blankEmail.setEmail("   ");

        emailService.sendActivityScheduledEmail(testActivity, Collections.singletonList(blankEmail));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    public void sendActivityScheduledEmail_emptyParticipantsList_sendsNoEmails() {
        emailService.sendActivityScheduledEmail(testActivity, Collections.emptyList());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    public void sendActivityScheduledEmail_weatherDependent_bodyContainsTempInfo() {
        testActivity.setWeatherDependent(true);
        testActivity.setMinTemp(5);
        testActivity.setMaxTemp(25);
        testActivity.setRainPreference(RainPreference.NoRain);

        List<User> participants = Collections.singletonList(userAlice);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();

        assertTrue(body.contains("5"));
        assertTrue(body.contains("25"));
        assertTrue(body.contains("NoRain"));
    }

    @Test
    public void sendActivityScheduledEmail_locationNull_showsTBD() {
        testActivity.setLocation(null);
        List<User> participants = Collections.singletonList(userAlice);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertTrue(captor.getValue().getText().contains("TBD"));
    }

    @Test
    public void sendActivityScheduledEmail_multipleParticipants_allUsernamesInBody() {
        List<User> participants = Arrays.asList(userAlice, userBob);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());

        boolean foundAlice = captor.getAllValues().stream().anyMatch(m -> m.getText().contains("alice"));
        boolean foundBob = captor.getAllValues().stream().anyMatch(m -> m.getText().contains("bob"));
        assertTrue(foundAlice);
        assertTrue(foundBob);
    }

    @Test
    public void sendActivityScheduledEmail_mixedEmailValidity_onlySendsToValid() {
        User noEmail = new User();
        noEmail.setUsername("ghost");
        noEmail.setEmail(null);

        List<User> participants = Arrays.asList(userAlice, noEmail, userBob);

        emailService.sendActivityScheduledEmail(testActivity, participants);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}