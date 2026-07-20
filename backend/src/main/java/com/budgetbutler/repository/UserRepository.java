package com.budgetbutler.repository;

import com.budgetbutler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Used during login: "find the user with this email, if one exists"
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
