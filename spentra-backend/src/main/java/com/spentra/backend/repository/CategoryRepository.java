package com.spentra.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spentra.backend.model.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

}
