package com.spentra.backend.model.entity;

import java.util.UUID;

import com.spentra.backend.model.enums.AuthProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a user in the system.
 * Houses credentials, identity names, email unique constraints, and auth providers.
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue
    @Column(name = "user_id")
    private UUID id;

    /**
     * Display name of the user.
     */
    private String name;

    /**
     * Unique email address used for registration and login.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * BCrypt-encoded password. Can be null for OAuth2 authenticated users.
     */
    @Column(name = "password", nullable = true)
    private String password;

    /**
     * Auth provider indicating registration source (e.g. LOCAL, GOOGLE).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;
}

