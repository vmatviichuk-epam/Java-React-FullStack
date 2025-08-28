package com.ecommerce.sportscenter.service;

import com.ecommerce.sportscenter.entity.PasswordHistory;
import com.ecommerce.sportscenter.entity.PasswordLogs;
import com.ecommerce.sportscenter.entity.User;
import com.ecommerce.sportscenter.repository.PasswordHistoryRepository;
import com.ecommerce.sportscenter.repository.PasswordLogsRepository;
import com.ecommerce.sportscenter.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordLogsRepository passwordLogsRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;

    private static final int PASSWORD_HISTORY_LIMIT = 5;
    private static final int PASSWORD_EXPIRY_MONTHS = 6;

    public PasswordService(UserRepository userRepository,
            PasswordHistoryRepository passwordHistoryRepository,
            PasswordLogsRepository passwordLogsRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordLogsRepository = passwordLogsRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = new PasswordValidationService();
    }

    @Transactional
    public void changePassword(String username, String newPassword, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate password complexity
        PasswordValidationService.ValidationResult validation = passwordValidationService.validatePassword(newPassword);
        if (!validation.isValid()) {
            throw new RuntimeException(
                    "Password does not meet requirements: " + String.join(", ", validation.getErrors()));
        }

        // Check password history
        if (isPasswordInHistory(user, newPassword)) {
            throw new RuntimeException("You cannot reuse any of your last 5 passwords");
        }

        // Store current password in history before changing
        if (user.getPassword() != null) {
            addPasswordToHistory(user, user.getPassword());
        }

        // Update user password and timestamp
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordUpdate(LocalDateTime.now());
        userRepository.save(user);

        // Log the password change
        logPasswordChange(user, ipAddress);

        // Clean up old password history entries
        cleanupPasswordHistory(user);
    }

    public boolean isPasswordExpired(User user) {
        if (user.getLastPasswordUpdate() == null) {
            // Legacy users - no expiration enforcement
            return false;
        }
        return user.getLastPasswordUpdate().isBefore(LocalDateTime.now().minusMonths(PASSWORD_EXPIRY_MONTHS));
    }

    public boolean needsMandatoryPasswordChange(User user, String rawPassword) {
        // Check if password meets current requirements
        PasswordValidationService.ValidationResult validation = passwordValidationService.validatePassword(rawPassword);

        // If user has never changed password (legacy user), allow login but note for
        // future enforcement
        if (user.getLastPasswordUpdate() == null) {
            return !validation.isValid();
        }

        // For users who have changed passwords before, enforce both complexity and
        // expiration
        return !validation.isValid() || isPasswordExpired(user);
    }

    private boolean isPasswordInHistory(User user, String newPassword) {
        List<PasswordHistory> recentPasswords = passwordHistoryRepository.findTop5ByUserOrderByCreatedAtDesc(user);
        return recentPasswords.stream()
                .anyMatch(ph -> passwordEncoder.matches(newPassword, ph.getPasswordHash()));
    }

    private void addPasswordToHistory(User user, String encodedPassword) {
        PasswordHistory passwordHistory = PasswordHistory.builder()
                .user(user)
                .passwordHash(encodedPassword)
                .build();
        passwordHistoryRepository.save(passwordHistory);
    }

    private void logPasswordChange(User user, String ipAddress) {
        PasswordLogs log = PasswordLogs.builder()
                .user(user)
                .action("PASSWORD_CHANGED")
                .ipAddress(ipAddress)
                .build();
        passwordLogsRepository.save(log);
    }

    private void cleanupPasswordHistory(User user) {
        List<PasswordHistory> allHistory = passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user);
        if (allHistory.size() > PASSWORD_HISTORY_LIMIT) {
            List<PasswordHistory> toDelete = allHistory.subList(PASSWORD_HISTORY_LIMIT, allHistory.size());
            passwordHistoryRepository.deleteAll(toDelete);
        }
    }

    public PasswordValidationService.ValidationResult validatePasswordComplexity(String password) {
        return passwordValidationService.validatePassword(password);
    }

    public List<String> getPasswordRequirements() {
        return passwordValidationService.getPasswordRequirements();
    }
}
