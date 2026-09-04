package com.gmail.messaging.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.messaging.model.Certificate;
import com.gmail.messaging.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class SqliteStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SqliteStorageService.class);
    private static final String LEGACY_STORAGE_FILE = "cluster_certificate_data.txt";
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SqliteStorageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initDatabase();
    }

    private synchronized void initDatabase() {
        try {
            jdbcTemplate.execute("PRAGMA foreign_keys = ON;");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS clusters (
                    id TEXT PRIMARY KEY,
                    cluster_name TEXT NOT NULL,
                    description TEXT,
                    recipient_emails TEXT
                );
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS certificates (
                    id TEXT PRIMARY KEY,
                    cluster_id TEXT NOT NULL,
                    certificate_name TEXT NOT NULL,
                    issued_date TEXT,
                    end_date TEXT,
                    days_remaining INTEGER,
                    status TEXT,
                    last_alert_sent_date TEXT,
                    FOREIGN KEY (cluster_id) REFERENCES clusters (id) ON DELETE CASCADE
                );
            """);

            logger.info("SQLite tables [clusters, certificates] initialized successfully.");

            Integer clusterCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clusters", Integer.class);
            if (clusterCount == null || clusterCount == 0) {
                migrateFromFileOrDefault();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize SQLite database tables:", e);
        }
    }

    private void migrateFromFileOrDefault() {
        boolean migratedFromFile = false;
        Path path = Paths.get(LEGACY_STORAGE_FILE);
        if (Files.exists(path)) {
            try {
                String jsonContent = Files.readString(path);
                if (jsonContent != null && !jsonContent.trim().isEmpty()) {
                    ObjectMapper mapper = new ObjectMapper();
                    List<Cluster> clustersFromFile = mapper.readValue(jsonContent, new TypeReference<List<Cluster>>() {});
                    if (clustersFromFile != null && !clustersFromFile.isEmpty()) {
                        saveClusters(clustersFromFile);
                        migratedFromFile = true;
                        logger.info("Migrated {} clusters from [{}] into SQLite database.", clustersFromFile.size(), LEGACY_STORAGE_FILE);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to migrate clusters from text file storage:", e);
            }
        }

        if (!migratedFromFile) {
            logger.info("Initializing default sample data in SQLite database...");
            List<Cluster> defaultClusters = createDefaultSampleData();
            saveClusters(defaultClusters);
        }
    }

    public synchronized List<Cluster> loadClusters() {
        String sqlClusters = "SELECT id, cluster_name, description, recipient_emails FROM clusters";
        List<Cluster> clusters = jdbcTemplate.query(sqlClusters, (rs, rowNum) -> {
            Cluster c = new Cluster();
            c.setId(rs.getString("id"));
            c.setClusterName(rs.getString("cluster_name"));
            c.setDescription(rs.getString("description"));
            c.setRecipientEmails(rs.getString("recipient_emails"));
            return c;
        });

        String sqlCerts = "SELECT id, cluster_id, certificate_name, issued_date, end_date, days_remaining, status, last_alert_sent_date FROM certificates WHERE cluster_id = ?";
        for (Cluster cluster : clusters) {
            List<Certificate> certs = jdbcTemplate.query(sqlCerts, (rs, rowNum) -> {
                Certificate cert = new Certificate();
                cert.setId(rs.getString("id"));
                cert.setCertificateName(rs.getString("certificate_name"));
                cert.setIssuedDate(rs.getString("issued_date"));
                cert.setEndDate(rs.getString("end_date"));
                cert.setDaysRemaining(rs.getLong("days_remaining"));
                cert.setStatus(rs.getString("status"));
                cert.setLastAlertSentDate(rs.getString("last_alert_sent_date"));
                return cert;
            }, cluster.getId());
            cluster.setCertificates(certs);
        }

        logger.info("Loaded {} clusters from SQLite database", clusters.size());
        return clusters;
    }

    public synchronized void saveClusters(List<Cluster> clusters) {
        if (clusters == null) return;
        for (Cluster cluster : clusters) {
            saveCluster(cluster);
        }
    }

    public synchronized void saveCluster(Cluster cluster) {
        if (cluster == null || cluster.getId() == null) return;
        String sqlCluster = "INSERT OR REPLACE INTO clusters (id, cluster_name, description, recipient_emails) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sqlCluster, cluster.getId(), cluster.getClusterName(), cluster.getDescription(), cluster.getRecipientEmails());

        if (cluster.getCertificates() != null) {
            for (Certificate cert : cluster.getCertificates()) {
                saveCertificate(cluster.getId(), cert);
            }
        }
    }

    public synchronized void saveCertificate(String clusterId, Certificate cert) {
        if (cert == null || cert.getId() == null) return;
        cert.recalculateStatus();
        String sqlCert = "INSERT OR REPLACE INTO certificates (id, cluster_id, certificate_name, issued_date, end_date, days_remaining, status, last_alert_sent_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sqlCert, cert.getId(), clusterId, cert.getCertificateName(), cert.getIssuedDate(), cert.getEndDate(), cert.getDaysRemaining(), cert.getStatus(), cert.getLastAlertSentDate());
    }

    public synchronized boolean deleteCluster(String clusterId) {
        jdbcTemplate.update("DELETE FROM certificates WHERE cluster_id = ?", clusterId);
        int rows = jdbcTemplate.update("DELETE FROM clusters WHERE id = ?", clusterId);
        logger.info("Deleted cluster [{}] from SQLite (rows affected: {})", clusterId, rows);
        return rows > 0;
    }

    public synchronized boolean deleteCertificate(String certId) {
        int rows = jdbcTemplate.update("DELETE FROM certificates WHERE id = ?", certId);
        logger.info("Deleted certificate [{}] from SQLite (rows affected: {})", certId, rows);
        return rows > 0;
    }

    private List<Cluster> createDefaultSampleData() {
        List<Cluster> list = new ArrayList<>();
        Cluster sample = new Cluster("cluster-1", "pucc", "pollution certification", "ananthakrishna810@gmail.com");
        sample.getCertificates().add(new Certificate("cert-1", "pollution certificate for KL00BD0000", "2026-08-04", "2026-08-26"));
        list.add(sample);
        return list;
    }
}
