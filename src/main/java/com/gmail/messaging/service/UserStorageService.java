package com.gmail.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserStorageService {

    private static final Logger logger = LoggerFactory.getLogger(UserStorageService.class);
    private static final String CREDENTIALS_FILE = "user_credentials.txt";
    private static final String DEFAULT_USERNAME = "ethan_hunt";
    private static final String DEFAULT_PASSWORD = "ethanhunt@10";

    public UserStorageService() {
        initFile();
    }

    private synchronized void initFile() {
        File file = new File(CREDENTIALS_FILE);
        if (!file.exists()) {
            try {
                String defaultContent = DEFAULT_USERNAME + ":" + DEFAULT_PASSWORD + "\n";
                Files.writeString(Path.of(CREDENTIALS_FILE), defaultContent);
                logger.info("Initialized default user credentials in [{}]", file.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to initialize user credentials file:", e);
            }
        }
    }

    public Map<String, String> loadAllUsers() {
        Map<String, String> users = new HashMap<>();
        Path path = Paths.get(CREDENTIALS_FILE);
        if (!Files.exists(path)) {
            initFile();
        }

        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    users.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            logger.error("Error reading credentials file:", e);
        }

        if (users.isEmpty()) {
            users.put(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        }

        return users;
    }

    public String getPasswordForUser(String username) {
        Map<String, String> users = loadAllUsers();
        return users.get(username);
    }
}
