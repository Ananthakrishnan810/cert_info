package com.gmail.messaging.controller;

import com.gmail.messaging.model.Certificate;
import com.gmail.messaging.model.Cluster;
import com.gmail.messaging.model.CreateCertificateRequest;
import com.gmail.messaging.model.CreateClusterRequest;
import com.gmail.messaging.service.ClusterService;
import com.gmail.messaging.service.GmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clusters")
@CrossOrigin(origins = "*")
public class ClusterController {

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
        return ResponseEntity.ok(cluster);
    }

    @PostMapping("/certificates")
    public ResponseEntity<Certificate> addCertificate(@Valid @RequestBody CreateCertificateRequest request) {
        Certificate cert = clusterService.addCertificateToCluster(
                request.getClusterId(),
                request.getCertificateName(),
                request.getIssuedDate(),
                request.getEndDate());
        return ResponseEntity.ok(cert);
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
        boolean deleted = clusterService.deleteCertificate(clusterId, certId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", deleted);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{clusterId}")
    public ResponseEntity<Map<String, Object>> deleteCluster(@PathVariable String clusterId) {
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
