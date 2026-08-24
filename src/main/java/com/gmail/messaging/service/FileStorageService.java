package com.gmail.messaging.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gmail.messaging.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final String STORAGE_FILE = "cluster_certificate_data.txt";
    private final ObjectMapper objectMapper;

    public FileStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        initStorageFile();
    }

    private void initStorageFile() {
        File file = new File(STORAGE_FILE);
        if (!file.exists()) {
            try {
                List<Cluster> defaultClusters = createDefaultSampleData();
                saveClusters(defaultClusters);
                logger.info("Initialized default sample clusters in storage file [{}]", file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Failed to initialize storage file:", e);
            }
        }
    }

    public synchronized List<Cluster> loadClusters() {
        Path path = Paths.get(STORAGE_FILE);
        if (!Files.exists(path)) {
            initStorageFile();
        }

        try {
            String jsonContent = Files.readString(path);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                return new ArrayList<>();
            }
            List<Cluster> clusters = objectMapper.readValue(jsonContent, new TypeReference<List<Cluster>>() {});
            logger.info("Successfully loaded {} clusters from [{}]", clusters.size(), STORAGE_FILE);
            return clusters;
        } catch (IOException e) {
            logger.error("Failed to read clusters from file storage [{}]", STORAGE_FILE, e);
            return new ArrayList<>();
        }
    }

    public synchronized void saveClusters(List<Cluster> clusters) {
        try {
            String jsonContent = objectMapper.writeValueAsString(clusters);
            Files.writeString(Paths.get(STORAGE_FILE), jsonContent);
            logger.info("Successfully persisted {} clusters to [{}]", clusters.size(), STORAGE_FILE);
        } catch (IOException e) {
            logger.error("Failed to write clusters to file storage [{}]", STORAGE_FILE, e);
        }
    }

    private List<Cluster> createDefaultSampleData() {
        List<Cluster> list = new ArrayList<>();
        Cluster sample = new Cluster("cluster-1", "pucc", "pollution certification", "ananthakrishna810@gmail.com");
        sample.getCertificates().add(new com.gmail.messaging.model.Certificate("cert-1", "pollution certificate for KL00BD0000", "2026-08-04", "2026-08-26"));
        list.add(sample);
        return list;
    }
}
