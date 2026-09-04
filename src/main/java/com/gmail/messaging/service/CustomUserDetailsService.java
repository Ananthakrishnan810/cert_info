package com.gmail.messaging.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserStorageService userStorageService;

    @Autowired
    public CustomUserDetailsService(UserStorageService userStorageService) {
        this.userStorageService = userStorageService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String hashedPassword = userStorageService.getPasswordForUser(username);
        if (hashedPassword == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return User.builder()
                .username(username)
                .password(hashedPassword)
                .roles("ADMIN")
                .build();
    }
}
