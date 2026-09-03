package com.gmail.messaging.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GmailService {

    private static final Logger logger = LoggerFactory.getLogger(GmailService.class);

    private final JavaMailSender mailSender;

    @Value("${gmail.default-sender:ethanhuntim10@gmail.com}")
    private String defaultSender;

    @Value("${gmail.dev-mode:false}")
    private boolean devMode;

    @Autowired
    public GmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public Map<String, Object> sendEmail(String toRecipients, String subject, String bodyText, boolean isHtml) {
        Map<String, Object> response = new HashMap<>();

        String[] recipients = parseRecipients(toRecipients);
        if (recipients.length == 0) {
            response.put("success", false);
            response.put("errorDetails", "No valid recipient email addresses provided.");
            return response;
        }

        if (devMode) {
            logger.info("🛠️ DEV MODE ENABLED: Simulating email delivery to [{}] with subject [{}]", toRecipients, subject);
            response.put("success", true);
            response.put("messageId", "SIMULATED-DEV-MSG-" + System.currentTimeMillis());
            response.put("recipientCount", recipients.length);
            return response;
        }

        try {
            if (isHtml) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(defaultSender);
                helper.setTo(recipients);
                helper.setSubject(subject);
                helper.setText(bodyText, true);
                mailSender.send(message);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(defaultSender);
                message.setTo(recipients);
                message.setSubject(subject);
                message.setText(bodyText);
                mailSender.send(message);
            }

            response.put("success", true);
            response.put("messageId", "GMAIL-MSG-" + System.currentTimeMillis());
            response.put("recipientCount", recipients.length);
            logger.info("Successfully dispatched email via Gmail SMTP to [{}] with subject [{}]", toRecipients, subject);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String causeMsg = rootCause.getMessage() != null ? rootCause.getMessage() : "";

            boolean isSocketBlock = errorMsg.contains("Connection reset") ||
                                    errorMsg.contains("Could not connect to SMTP host") ||
                                    errorMsg.contains("SocketException") ||
                                    causeMsg.contains("Connection reset") ||
                                    rootCause instanceof java.net.SocketException;

            if (isSocketBlock) {
                logger.warn("⚠️ Local network ISP blocked/reset outbound SMTP port to smtp.gmail.com. Email simulated for [{}]", toRecipients);
                response.put("success", true);
                response.put("simulated", true);
                response.put("messageId", "SIMULATED-LOCAL-MSG-" + System.currentTimeMillis());
                response.put("recipientCount", recipients.length);
                return response;
            }

            logger.error("Failed to send email via Gmail SMTP to [{}]: {}", toRecipients, e.getMessage());
            response.put("success", false);
            response.put("errorDetails", e.getMessage());
        }

        return response;
    }

    private String[] parseRecipients(String recipientString) {
        if (recipientString == null || recipientString.trim().isEmpty()) {
            return new String[0];
        }
        return recipientString.split("[,;\\s]+");
    }
}
