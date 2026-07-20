package com.budgetbutler.repository;

import com.budgetbutler.model.Garden;
import com.budgetbutler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GardenRepository extends JpaRepository<Garden, Long> {

    Optional<Garden> findByOwner(User owner);
}
