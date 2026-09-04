package com.gmail.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserStorageService {

    private static final Logger logger = LoggerFactory.getLogger(UserStorageService.class);
    private static final String LEGACY_CREDENTIALS_FILE = "user_credentials.txt";
    private static final String DEFAULT_USERNAME = "ethan_hunt";
    private static final String DEFAULT_PASSWORD = "ethanhunt@10";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserStorageService(JdbcTemplate jdbcTemplate, @Lazy PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        initDatabase();
    }

    private synchronized void initDatabase() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password TEXT NOT NULL
                );
            """);
            logger.info("SQLite table [users] initialized successfully.");

            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            if (count == null || count == 0) {
                migrateFromFileOrDefault();
            } else {
                ensurePasswordsHashed();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize SQLite users table:", e);
        }
    }

    private void migrateFromFileOrDefault() {
        boolean migrated = false;
        Path path = Paths.get(LEGACY_CREDENTIALS_FILE);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        saveUser(parts[0].trim(), parts[1].trim());
                        migrated = true;
                    }
                }
                if (migrated) {
                    logger.info("Migrated user credentials into SQLite with BCrypt hashing.");
                }
            } catch (Exception e) {
                logger.error("Failed to migrate user credentials from file to SQLite:", e);
            }
        }

        if (!migrated) {
            saveUser(DEFAULT_USERNAME, DEFAULT_PASSWORD);
            logger.info("Initialized default user [{}] with BCrypt password hashing in SQLite.", DEFAULT_USERNAME);
        }
    }

    private void ensurePasswordsHashed() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT username, password FROM users");
        for (Map<String, Object> row : rows) {
            String username = (String) row.get("username");
            String storedPassword = (String) row.get("password");
            if (storedPassword != null && !storedPassword.startsWith("$2a$") && !storedPassword.startsWith("$2b$") && !storedPassword.startsWith("$2y$")) {
                String hashedPassword = passwordEncoder.encode(storedPassword);
                jdbcTemplate.update("UPDATE users SET password = ? WHERE username = ?", hashedPassword, username);
                logger.info("Encrypted plain-text password with BCrypt for user [{}] in SQLite", username);
            }
        }
    }

    public synchronized Map<String, String> loadAllUsers() {
        Map<String, String> users = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT username, password FROM users");
            for (Map<String, Object> row : rows) {
                users.put((String) row.get("username"), (String) row.get("password"));
            }
        } catch (Exception e) {
            logger.error("Error fetching users from SQLite:", e);
        }

        return users;
    }

    public synchronized String getPasswordForUser(String username) {
        if (username == null || username.isBlank()) return null;
        try {
            List<String> results = jdbcTemplate.query(
                    "SELECT password FROM users WHERE username = ?",
                    (rs, rowNum) -> rs.getString("password"),
                    username
            );
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error querying password for user [{}] from SQLite:", username, e);
            return null;
        }
    }

    public synchronized void saveUser(String username, String rawOrHashedPassword) {
        String hashedPassword = rawOrHashedPassword;
        if (rawOrHashedPassword != null && !rawOrHashedPassword.startsWith("$2a$") && !rawOrHashedPassword.startsWith("$2b$") && !rawOrHashedPassword.startsWith("$2y$")) {
            hashedPassword = passwordEncoder.encode(rawOrHashedPassword);
        }
        jdbcTemplate.update("INSERT OR REPLACE INTO users (username, password) VALUES (?, ?)", username, hashedPassword);
    }
}
