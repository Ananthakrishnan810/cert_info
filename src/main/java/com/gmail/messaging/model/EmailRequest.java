package com.gmail.messaging.model;

import jakarta.validation.constraints.NotBlank;

public class EmailRequest {

    @NotBlank(message = "Recipient email 'to' is required")
    private String to;

    @NotBlank(message = "Email 'subject' is required")
    private String subject;

    @NotBlank(message = "Email 'body' is required")
    private String body;

    private boolean isHtml = false;

    public EmailRequest() {}

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isHtml() { return isHtml; }
    public void setHtml(boolean html) { isHtml = html; }
}
