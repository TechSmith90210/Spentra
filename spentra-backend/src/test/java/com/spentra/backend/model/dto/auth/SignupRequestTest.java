package com.spentra.backend.model.dto.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class SignupRequestTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private SignupRequest createValidSignupRequest() {
        SignupRequest request = new SignupRequest();
        request.setEmail("user@example.com");
        request.setPassword("Password123!");
        request.setConfirmPassword("Password123!");
        request.setName("Test User");
        return request;
    }

    @Test
    public void testSignupRequest_WithValidSpecialCharactersPassword() {
        SignupRequest request = createValidSignupRequest();
        request.setPassword("P@ssword123!");
        request.setConfirmPassword("P@ssword123!");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid password with special characters should not have validation violations.");
    }

    @Test
    public void testSignupRequest_WithValidAlphanumericPassword() {
        SignupRequest request = createValidSignupRequest();
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid alphanumeric password should not have validation violations.");
    }

    @Test
    public void testSignupRequest_WithTooShortPassword() {
        SignupRequest request = createValidSignupRequest();
        request.setPassword("P1!"); // Less than 8 chars
        request.setConfirmPassword("P1!");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Password shorter than 8 characters should trigger validation errors.");
    }

    @Test
    public void testSignupRequest_WithNoDigits() {
        SignupRequest request = createValidSignupRequest();
        request.setPassword("Password!"); // No digit
        request.setConfirmPassword("Password!");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Password with no digits should trigger validation errors.");
    }

    @Test
    public void testSignupRequest_WithNoLetters() {
        SignupRequest request = createValidSignupRequest();
        request.setPassword("12345678!"); // No letters
        request.setConfirmPassword("12345678!");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Password with no letters should trigger validation errors.");
    }

    @Test
    public void testSignupRequest_WithInvalidEmail() {
        SignupRequest request = createValidSignupRequest();
        request.setEmail("invalid-email-format");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Invalid email address format should trigger validation errors.");
    }
}
