package com.spentra.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spentra.backend.model.entity.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

}