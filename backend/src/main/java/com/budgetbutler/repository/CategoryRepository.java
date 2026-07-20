package com.budgetbutler.repository;

import com.budgetbutler.model.Category;
import com.budgetbutler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * By extending JpaRepository, we instantly get methods like:
 * save(), findAll(), findById(), deleteById() ... without writing any SQL!
 * <Category, Long> means: "this repository manages Category entities, whose id is a Long".
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Spring builds the SQL automatically from the method name: WHERE owner = ?
    List<Category> findByOwner(User owner);
}
