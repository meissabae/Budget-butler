package com.budgetbutler.repository;

import com.budgetbutler.model.Dream;
import com.budgetbutler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DreamRepository extends JpaRepository<Dream, Long> {

    List<Dream> findByOwner(User owner);
}
