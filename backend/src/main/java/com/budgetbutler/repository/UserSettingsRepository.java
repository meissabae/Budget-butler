package com.budgetbutler.repository;

import com.budgetbutler.model.User;
import com.budgetbutler.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByOwner(User owner);
}
