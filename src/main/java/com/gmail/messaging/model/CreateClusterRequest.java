package com.gmail.messaging.model;

import jakarta.validation.constraints.NotBlank;

public class CreateClusterRequest {
    @NotBlank(message = "Cluster name is required")
    private String clusterName;

    private String description;

    @NotBlank(message = "Recipient emails are required")
    private String recipientEmails;

    public CreateClusterRequest() {}

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRecipientEmails() { return recipientEmails; }
    public void setRecipientEmails(String recipientEmails) { this.recipientEmails = recipientEmails; }
}
