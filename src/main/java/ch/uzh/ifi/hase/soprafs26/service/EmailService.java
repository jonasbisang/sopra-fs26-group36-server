package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Activity;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendActivityScheduledEmail(Activity activity, List<User> participants) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String formattedTime = activity.getScheduledTime().format(formatter);

        String participantNames = participants.stream()
                .map(User::getUsername)
                .reduce((a, b) -> a + ", " + b)
                .orElse("No participants");

        String subject = "🎉 Activity Scheduled: " + activity.getName();

        StringBuilder body = new StringBuilder();
        body.append("Hey! Your activity has been scheduled.\n\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        body.append("📌 Activity: ").append(activity.getName()).append("\n");
        body.append("📅 Date & Time: ").append(formattedTime).append("\n");
        body.append("📍 Location: ").append(activity.getLocation() != null ? activity.getLocation() : "TBD").append("\n");
        body.append("⏱ Duration: ").append(activity.getDuration()).append(" hour(s)\n");
        body.append("👥 Participants: ").append(participantNames).append("\n");

        if (activity.isWeatherDependent()) {
            body.append("\n🌤 Weather Requirements:\n");
            body.append("   Min Temperature: ").append(activity.getMinTemp()).append("°C\n");
            body.append("   Max Temperature: ").append(activity.getMaxTemp()).append("°C\n");
            if (activity.getRainPreference() != null) {
                body.append("   Rain Preference: ").append(activity.getRainPreference()).append("\n");
            }
        }

        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        body.append("\nSee you there! 🚀\n");
        body.append("— The Friendler Team");

        for (User participant : participants) {
            if (participant.getEmail() != null && !participant.getEmail().isBlank()) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(participant.getEmail());
                message.setSubject(subject);
                message.setText(body.toString());
                mailSender.send(message);
            }
        }
    }
}

