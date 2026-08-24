package com.gmail.messaging.model;

import jakarta.validation.constraints.NotBlank;

public class CreateCertificateRequest {
    @NotBlank(message = "Cluster ID is required")
    private String clusterId;

    @NotBlank(message = "Certificate name is required")
    private String certificateName;

    @NotBlank(message = "Issued date is required")
    private String issuedDate;

    @NotBlank(message = "End date is required")
    private String endDate;

    public CreateCertificateRequest() {}

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getCertificateName() { return certificateName; }
    public void setCertificateName(String certificateName) { this.certificateName = certificateName; }

    public String getIssuedDate() { return issuedDate; }
    public void setIssuedDate(String issuedDate) { this.issuedDate = issuedDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
