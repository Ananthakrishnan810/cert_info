package com.gmail.messaging.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserStorageService userStorageService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomUserDetailsService(UserStorageService userStorageService, @Lazy PasswordEncoder passwordEncoder) {
        this.userStorageService = userStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String rawPassword = userStorageService.getPasswordForUser(username);
        if (rawPassword == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles("ADMIN")
                .build();
    }
}
