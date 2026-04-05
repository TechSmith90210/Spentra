package com.spentra.backend.model.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Expenses")
public class Expense {

    @Id
    @GeneratedValue
    private UUID id;

    private String title;
    private Double amount;
}
