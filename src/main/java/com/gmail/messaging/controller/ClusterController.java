package com.gmail.messaging.controller;

import com.gmail.messaging.model.Certificate;
import com.gmail.messaging.model.Cluster;
import com.gmail.messaging.model.CreateCertificateRequest;
import com.gmail.messaging.model.CreateClusterRequest;
import com.gmail.messaging.service.ClusterService;
import com.gmail.messaging.service.GmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/clusters")
@CrossOrigin(origins = "*")
public class ClusterController {

    private static final Logger logger = LoggerFactory.getLogger(ClusterController.class);

    private final ClusterService clusterService;
    private final GmailService gmailService;

    @Autowired
    public ClusterController(ClusterService clusterService, GmailService gmailService) {
        this.clusterService = clusterService;
        this.gmailService = gmailService;
    }

    @GetMapping
    public ResponseEntity<List<Cluster>> getAllClusters() {
        return ResponseEntity.ok(clusterService.getAllClusters());
    }

    @PostMapping
    public ResponseEntity<Cluster> createCluster(@Valid @RequestBody CreateClusterRequest request) {
        Cluster cluster = clusterService.createCluster(request.getClusterName(), request.getDescription(),
                request.getRecipientEmails());
        CompletableFuture.runAsync(() -> sendWelcomeEmail(cluster));
        return ResponseEntity.ok(cluster);
    }

    @PutMapping("/{clusterId}")
    public ResponseEntity<Cluster> updateCluster(
            @PathVariable String clusterId,
            @Valid @RequestBody CreateClusterRequest request) {
        Cluster updated = clusterService.updateCluster(
                clusterId,
                request.getClusterName(),
                request.getDescription(),
                request.getRecipientEmails());
        return ResponseEntity.ok(updated);
    }

    private void sendWelcomeEmail(Cluster cluster) {
        if (cluster.getRecipientEmails() == null || cluster.getRecipientEmails().isBlank()) {
            return;
        }
        String subject = "Welcome to Certificate Monitoring Cluster - " + cluster.getClusterName();
        String bodyHtml = String.format("""
                <div style="font-family: 'Inter', sans-serif; max-width: 600px; padding: 24px; background: #121a2b; color: #f8fafc; border-radius: 12px; border: 1px solid #1e293b;">
                    <h2 style="color: #3b82f6; margin-top: 0;">🎉 Welcome to Certificate Monitoring</h2>
                    <p>Your email address has been registered to receive certificate expiration alerts for cluster <strong>%s</strong>.</p>
                    <div style="background: rgba(255,255,255,0.04); padding: 16px; border-radius: 8px; margin: 16px 0; border-left: 4px solid #3b82f6;">
                        <p style="margin: 4px 0; color: #94a3b8;"><strong>Cluster Name:</strong> <span style="color: #ffffff;">%s</span></p>
                        <p style="margin: 4px 0; color: #94a3b8;"><strong>Description:</strong> <span style="color: #ffffff;">%s</span></p>
                        <p style="margin: 4px 0; color: #94a3b8;"><strong>Registered Recipient(s):</strong> <span style="color: #38bdf8;">%s</span></p>
                    </div>
                    <p style="color: #94a3b8; font-size: 0.85rem;">You will automatically receive notification alerts prior to certificate expiration dates.</p>
                </div>
                """,
                cluster.getClusterName(),
                cluster.getClusterName(),
                cluster.getDescription() != null ? cluster.getDescription() : "N/A",
                cluster.getRecipientEmails()
        );

        try {
            logger.info("Sending welcome email for new cluster [{}] to [{}]", cluster.getClusterName(), cluster.getRecipientEmails());
            Map<String, Object> result = gmailService.sendEmail(cluster.getRecipientEmails(), subject, bodyHtml, true);
            if (Boolean.TRUE.equals(result.get("success"))) {
                logger.info("Welcome email sent successfully for cluster [{}]", cluster.getClusterName());
            } else {
                logger.warn("Welcome email could not be sent: {}", result.get("errorDetails"));
            }
        } catch (Throwable e) {
            logger.error("Failed to send welcome email for cluster [{}]: {}", cluster.getClusterName(), e.getMessage());
        }
    }

    @PostMapping("/certificates")
    public ResponseEntity<Certificate> addCertificate(@Valid @RequestBody CreateCertificateRequest request) {
        Certificate cert = clusterService.addCertificateToCluster(
                request.getClusterId(),
                request.getCertificateName(),
                request.getIssuedDate(),
                request.getEndDate());
        CompletableFuture.runAsync(() -> sendCertificateCreatedWelcomeEmail(request.getClusterId(), cert));
        return ResponseEntity.ok(cert);
    }

    private void sendCertificateCreatedWelcomeEmail(String clusterId, Certificate cert) {
        Cluster cluster = clusterService.findClusterById(clusterId);
        if (cluster == null || cluster.getRecipientEmails() == null || cluster.getRecipientEmails().isBlank()) {
            return;
        }

        String subject = "🎉 New Certificate Created: " + cert.getCertificateName() + " (Cluster: " + cluster.getClusterName() + ")";
        String bodyHtml = String.format("""
                <div style="font-family: 'Inter', sans-serif; max-width: 600px; padding: 24px; background: #121a2b; color: #f8fafc; border-radius: 12px; border: 1px solid #1e293b;">
                    <h2 style="color: #3b82f6; margin-top: 0;">📜 Certificate Registered Welcome Notification</h2>
                    <p>A new certificate has been registered under cluster <strong>%s</strong> and added to your expiration monitoring watchlist.</p>
                    
                    <div style="background: rgba(255,255,255,0.04); padding: 18px; border-radius: 8px; margin: 16px 0; border-left: 4px solid #38bdf8;">
                        <h4 style="margin: 0 0 12px 0; color: #38bdf8; font-size: 1rem;">Certificate Details</h4>
                        <p style="margin: 6px 0; color: #94a3b8;"><strong>Certificate Name:</strong> <span style="color: #ffffff;">%s</span></p>
                        <p style="margin: 6px 0; color: #94a3b8;"><strong>Target Cluster:</strong> <span style="color: #ffffff;">%s</span></p>
                        <p style="margin: 6px 0; color: #94a3b8;"><strong>Issued Date:</strong> <span style="color: #ffffff;">%s</span></p>
                        <p style="margin: 6px 0; color: #94a3b8;"><strong>Expiry Date:</strong> <span style="color: #f59e0b;">%s</span></p>
                        <p style="margin: 6px 0; color: #94a3b8;"><strong>Days Remaining:</strong> <span style="color: #38bdf8;">%d day(s)</span></p>
                        <p style="margin: 6px 0; color: #94a3b8;"><strong>Current Status:</strong> <span style="color: #10b981; font-weight: 600;">%s</span></p>
                    </div>

                    <div style="background: rgba(255,255,255,0.02); padding: 14px; border-radius: 8px; margin-top: 16px; border: 1px solid #334155;">
                        <p style="margin: 4px 0; color: #94a3b8; font-size: 0.85rem;"><strong>Registered Recipients:</strong> %s</p>
                    </div>
                    
                    <p style="color: #64748b; font-size: 0.8rem; margin-top: 20px;">You will automatically receive automated email notifications prior to the certificate expiry date.</p>
                </div>
                """,
                cluster.getClusterName(),
                cert.getCertificateName(),
                cluster.getClusterName(),
                cert.getIssuedDate(),
                cert.getEndDate(),
                cert.getDaysRemaining(),
                cert.getStatus() != null ? cert.getStatus() : "ACTIVE",
                cluster.getRecipientEmails()
        );

        try {
            logger.info("Sending certificate welcome email for cert [{}] in cluster [{}] to [{}]", cert.getCertificateName(), cluster.getClusterName(), cluster.getRecipientEmails());
            Map<String, Object> result = gmailService.sendEmail(cluster.getRecipientEmails(), subject, bodyHtml, true);
            if (Boolean.TRUE.equals(result.get("success"))) {
                logger.info("Certificate welcome email sent successfully for cert [{}]", cert.getCertificateName());
            } else {
                logger.warn("Certificate welcome email could not be sent: {}", result.get("errorDetails"));
            }
        } catch (Throwable e) {
            logger.error("Failed to send certificate welcome email for cert [{}]: {}", cert.getCertificateName(), e.getMessage());
        }
    }

    @PutMapping("/{clusterId}/certificates/{certId}")
    public ResponseEntity<Certificate> updateCertificate(
            @PathVariable String clusterId,
            @PathVariable String certId,
            @Valid @RequestBody CreateCertificateRequest request) {
        Certificate updated = clusterService.updateCertificate(
                clusterId,
                certId,
                request.getCertificateName(),
                request.getIssuedDate(),
                request.getEndDate());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{clusterId}/certificates/{certId}")
    public ResponseEntity<Map<String, Object>> deleteCertificate(@PathVariable String clusterId,
            @PathVariable String certId) {
        logger.info("REST request to delete certificate ID: [{}] from cluster ID: [{}]", certId, clusterId);
        boolean deleted = clusterService.deleteCertificate(clusterId, certId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", deleted);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{clusterId}")
    public ResponseEntity<Map<String, Object>> deleteCluster(@PathVariable String clusterId) {
        logger.info("REST request to delete cluster ID: [{}]", clusterId);
        boolean deleted = clusterService.deleteCluster(clusterId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", deleted);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/check-expirations")
    public ResponseEntity<Map<String, Object>> triggerExpirationCheck(
            @RequestParam(defaultValue = "false") boolean forceAlert) {
        int triggered = clusterService.checkAndTriggerExpirationAlerts(gmailService, forceAlert);
        Map<String, Object> res = new HashMap<>();
        res.put("triggeredAlerts", triggered);
        res.put("message", "Expiration check completed successfully.");
        return ResponseEntity.ok(res);
    }
}
