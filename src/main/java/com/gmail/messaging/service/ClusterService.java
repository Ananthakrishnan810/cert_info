package com.gmail.messaging.service;

import com.gmail.messaging.model.Certificate;
import com.gmail.messaging.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClusterService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterService.class);

    private final FileStorageService fileStorageService;
    private final List<Cluster> clusters;

    @Autowired
    public ClusterService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
        this.clusters = fileStorageService.loadClusters();
        logger.info("ClusterService initialized with {} clusters", clusters.size());
    }

    public synchronized List<Cluster> getAllClusters() {
        for (Cluster cluster : clusters) {
            if (cluster.getCertificates() != null) {
                for (Certificate cert : cluster.getCertificates()) {
                    cert.recalculateStatus();
                }
            }
        }
        return new ArrayList<>(clusters);
    }

    public synchronized Cluster createCluster(String name, String description, String recipientEmails) {
        String id = "cluster-" + UUID.randomUUID().toString().substring(0, 8);
        Cluster newCluster = new Cluster(id, name, description, recipientEmails);
        clusters.add(newCluster);
        fileStorageService.saveClusters(clusters);
        return newCluster;
    }

    public synchronized Cluster updateCluster(String clusterId, String name, String description, String recipientEmails) {
        Cluster cluster = findClusterById(clusterId);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster with ID " + clusterId + " not found.");
        }
        cluster.setClusterName(name);
        cluster.setDescription(description);
        cluster.setRecipientEmails(recipientEmails);

        fileStorageService.saveClusters(clusters);
        return cluster;
    }

    public synchronized Certificate addCertificateToCluster(String clusterId, String certName, String issuedDate, String endDate) {
        Cluster cluster = findClusterById(clusterId);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster with ID " + clusterId + " not found.");
        }

        String id = "cert-" + UUID.randomUUID().toString().substring(0, 8);
        Certificate cert = new Certificate(id, certName, issuedDate, endDate);
        if (cluster.getCertificates() == null) {
            cluster.setCertificates(new ArrayList<>());
        }
        cluster.getCertificates().add(cert);
        fileStorageService.saveClusters(clusters);
        return cert;
    }

    public synchronized Certificate updateCertificate(String clusterId, String certId, String certName, String issuedDate, String endDate) {
        Cluster cluster = findClusterById(clusterId);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster with ID " + clusterId + " not found.");
        }

        Certificate cert = findCertInCluster(cluster, certId);
        if (cert == null) {
            throw new IllegalArgumentException("Certificate with ID " + certId + " not found in cluster " + clusterId);
        }

        cert.setCertificateName(certName);
        cert.setIssuedDate(issuedDate);
        cert.setEndDate(endDate);
        cert.recalculateStatus();

        fileStorageService.saveClusters(clusters);
        return cert;
    }

    public synchronized boolean deleteCertificate(String clusterId, String certId) {
        Cluster cluster = findClusterById(clusterId);
        if (cluster != null && cluster.getCertificates() != null) {
            boolean removed = cluster.getCertificates().removeIf(c -> c.getId() != null && c.getId().equals(certId));
            if (removed) {
                fileStorageService.saveClusters(clusters);
                logger.info("Successfully deleted cert [{}] from cluster [{}]", certId, clusterId);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean deleteCluster(String clusterId) {
        logger.info("Attempting to delete cluster with ID [{}]", clusterId);
        boolean removed = clusters.removeIf(c -> c.getId() != null && c.getId().equals(clusterId));
        if (removed) {
            fileStorageService.saveClusters(clusters);
            logger.info("Successfully deleted cluster [{}] and saved storage file.", clusterId);
            return true;
        }
        logger.warn("Failed to delete cluster [{}]: cluster ID not found.", clusterId);
        return false;
    }

    public synchronized int checkAndTriggerExpirationAlerts(GmailService gmailService, boolean forceAlert) {
        int alertCount = 0;
        String todayStr = LocalDate.now().toString();

        for (Cluster cluster : clusters) {
            if (cluster.getCertificates() == null || cluster.getCertificates().isEmpty()) {
                continue;
            }

            for (Certificate cert : cluster.getCertificates()) {
                cert.recalculateStatus();
                long daysRemaining = cert.getDaysRemaining();

                if (daysRemaining <= 7) {
                    boolean alreadySentToday = todayStr.equals(cert.getLastAlertSentDate());

                    if (forceAlert || !alreadySentToday) {
                        String recipients = cluster.getRecipientEmails();
                        String subject = "URGENT: Certificate Expiration Alert - " + cert.getCertificateName();
                        String bodyHtml = String.format("""
                                <div style="font-family: 'Inter', sans-serif; max-width: 600px; padding: 24px; background: #121a2b; color: #f8fafc; border-radius: 12px; border: 1px solid #1e293b;">
                                    <h2 style="color: #ef4444; margin-top: 0;">⚠️ Certificate Expiration Alert</h2>
                                    <p>The following certificate assigned to cluster <strong>%s</strong> is nearing expiration:</p>
                                    <table style="width: 100%%; border-collapse: collapse; margin: 16px 0; background: rgba(255,255,255,0.04); border-radius: 8px;">
                                        <tr><td style="padding: 10px; color: #94a3b8;">Certificate Name:</td><td style="padding: 10px; font-weight: bold; color: #ffffff;">%s</td></tr>
                                        <tr><td style="padding: 10px; color: #94a3b8;">Cluster:</td><td style="padding: 10px; color: #ffffff;">%s</td></tr>
                                        <tr><td style="padding: 10px; color: #94a3b8;">Expiration Date:</td><td style="padding: 10px; color: #ef4444; font-weight: bold;">%s</td></tr>
                                        <tr><td style="padding: 10px; color: #94a3b8;">Days Remaining:</td><td style="padding: 10px; color: #f59e0b; font-weight: bold;">%d Day(s)</td></tr>
                                    </table>
                                    <p style="color: #94a3b8; font-size: 0.85rem;">This automated alert is generated daily by Certificate Manager.</p>
                                </div>
                                """,
                                cluster.getClusterName(),
                                cert.getCertificateName(),
                                cluster.getClusterName(),
                                cert.getEndDate(),
                                daysRemaining
                        );

                        logger.info("Triggering expiration email for cert [{}] in cluster [{}] to [{}]", cert.getCertificateName(), cluster.getClusterName(), recipients);
                        gmailService.sendEmail(recipients, subject, bodyHtml, true);

                        cert.setLastAlertSentDate(todayStr);
                        alertCount++;
                    }
                }
            }
        }

        if (alertCount > 0) {
            fileStorageService.saveClusters(clusters);
        }

        return alertCount;
    }

    public Cluster findClusterById(String id) {
        return clusters.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private Certificate findCertInCluster(Cluster cluster, String certId) {
        if (cluster.getCertificates() == null) return null;
        return cluster.getCertificates().stream().filter(c -> c.getId().equals(certId)).findFirst().orElse(null);
    }
}
