package com.ecommerce.sportscenter.service;

import com.ecommerce.sportscenter.entity.User;
import com.ecommerce.sportscenter.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DataInitializationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initializeData() {
        // Create default user if no users exist
        if (userRepository.count() == 0) {
            User defaultUser = User.builder()
                    .username("rahul")
                    .password(passwordEncoder.encode("Password"))
                    .email("rahul@example.com")
                    .role("ADMIN")
                    .lastPasswordUpdate(null) // Set to null for legacy user behavior
                    .build();

            userRepository.save(defaultUser);
        }
    }
}
