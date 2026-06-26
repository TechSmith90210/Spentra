package com.spentra.backend.model.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a transaction category.
 * Can represent a system-wide global category (user is null) or a user-specific custom category.
 */
@Getter
@Setter
@Entity
public class Category {

    /**
     * Unique identifier for the category.
     */
    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Display name of the category.
     */
    private String name;

    /**
     * The owner of this custom category.
     * If null, this is a system-wide global category accessible by all users.
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

