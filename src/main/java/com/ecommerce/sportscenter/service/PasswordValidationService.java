package com.ecommerce.sportscenter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PasswordValidationService {

    private static final int MIN_LENGTH = 8;
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*");

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> requirements;

        public ValidationResult(boolean valid, List<String> errors, List<String> requirements) {
            this.valid = valid;
            this.errors = errors;
            this.requirements = requirements;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getRequirements() {
            return requirements;
        }
    }

    public ValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        List<String> requirements = new ArrayList<>();

        // Minimum length requirement
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least 8 characters long");
            requirements.add("✗ At least 8 characters long");
        } else {
            requirements.add("✓ At least 8 characters long");
        }

        // Lowercase letter requirement
        if (password == null || !LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least 1 lowercase letter (a-z)");
            requirements.add("✗ Contains at least 1 lowercase letter (a-z)");
        } else {
            requirements.add("✓ Contains at least 1 lowercase letter (a-z)");
        }

        // Uppercase letter requirement
        if (password == null || !UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least 1 uppercase letter (A-Z)");
            requirements.add("✗ Contains at least 1 uppercase letter (A-Z)");
        } else {
            requirements.add("✓ Contains at least 1 uppercase letter (A-Z)");
        }

        // Digit requirement
        if (password == null || !DIGIT_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least 1 number (0-9)");
            requirements.add("✗ Contains at least 1 number (0-9)");
        } else {
            requirements.add("✓ Contains at least 1 number (0-9)");
        }

        // Special character requirement
        if (password == null || !SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least 1 special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
            requirements.add("✗ Contains at least 1 special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        } else {
            requirements.add("✓ Contains at least 1 special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }

        return new ValidationResult(errors.isEmpty(), errors, requirements);
    }

    public List<String> getPasswordRequirements() {
        List<String> requirements = new ArrayList<>();
        requirements.add("✓ At least 8 characters long");
        requirements.add("✓ Contains at least 1 lowercase letter (a-z)");
        requirements.add("✓ Contains at least 1 uppercase letter (A-Z)");
        requirements.add("✓ Contains at least 1 number (0-9)");
        requirements.add("✓ Contains at least 1 special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        requirements.add("✓ Cannot be one of your last 5 passwords");
        return requirements;
    }
}
