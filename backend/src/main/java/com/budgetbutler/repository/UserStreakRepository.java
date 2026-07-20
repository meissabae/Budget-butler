package com.budgetbutler.repository;

import com.budgetbutler.model.User;
import com.budgetbutler.model.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {

    Optional<UserStreak> findByOwner(User owner);
}
