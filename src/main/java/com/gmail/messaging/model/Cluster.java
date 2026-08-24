package com.gmail.messaging.model;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private String id;
    private String clusterName;
    private String description;
    private String recipientEmails;
    private List<Certificate> certificates = new ArrayList<>();

    public Cluster() {}

    public Cluster(String id, String clusterName, String description, String recipientEmails) {
        this.id = id;
        this.clusterName = clusterName;
        this.description = description;
        this.recipientEmails = recipientEmails;
        this.certificates = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRecipientEmails() { return recipientEmails; }
    public void setRecipientEmails(String recipientEmails) { this.recipientEmails = recipientEmails; }

    public List<Certificate> getCertificates() { return certificates; }
    public void setCertificates(List<Certificate> certificates) { this.certificates = certificates; }
}
