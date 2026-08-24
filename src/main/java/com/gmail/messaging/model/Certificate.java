package com.gmail.messaging.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Certificate {
    private String id;
    private String certificateName;
    private String issuedDate;
    private String endDate;
    private long daysRemaining;
    private String status;
    private String lastAlertSentDate;

    public Certificate() {}

    public Certificate(String id, String certificateName, String issuedDate, String endDate) {
        this.id = id;
        this.certificateName = certificateName;
        this.issuedDate = issuedDate;
        this.endDate = endDate;
        recalculateStatus();
    }

    public void recalculateStatus() {
        if (endDate != null && !endDate.isEmpty()) {
            try {
                LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDate now = LocalDate.now();
                this.daysRemaining = ChronoUnit.DAYS.between(now, end);

                if (daysRemaining < 0) {
                    this.status = "EXPIRED (" + Math.abs(daysRemaining) + " days ago)";
                } else if (daysRemaining == 0) {
                    this.status = "EXPIRES TODAY";
                } else if (daysRemaining <= 7) {
                    this.status = "EXPIRING (" + daysRemaining + " Days)";
                } else {
                    this.status = "ACTIVE";
                }
            } catch (Exception e) {
                this.status = "UNKNOWN";
            }
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCertificateName() { return certificateName; }
    public void setCertificateName(String certificateName) { this.certificateName = certificateName; }

    public String getIssuedDate() { return issuedDate; }
    public void setIssuedDate(String issuedDate) { this.issuedDate = issuedDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
        recalculateStatus();
    }

    public long getDaysRemaining() {
        recalculateStatus();
        return daysRemaining;
    }
    public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }

    public String getStatus() {
        recalculateStatus();
        return status;
    }
    public void setStatus(String status) { this.status = status; }

    public String getLastAlertSentDate() { return lastAlertSentDate; }
    public void setLastAlertSentDate(String lastAlertSentDate) { this.lastAlertSentDate = lastAlertSentDate; }
}
