package com.budgetbutler.controller;

import com.budgetbutler.dto.CategoryRequest;
import com.budgetbutler.dto.CategoryResponse;
import com.budgetbutler.model.Category;
import com.budgetbutler.model.User;
import com.budgetbutler.model.Wallet;
import com.budgetbutler.repository.CategoryRepository;
import com.budgetbutler.repository.WalletRepository;
import com.budgetbutler.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @RestController = this class handles HTTP requests and returns JSON (not HTML pages).
 * @RequestMapping("/api/categories") = every method here starts with that URL prefix.
 * Every method below is scoped to the logged-in user - nobody can see or edit
 * another user's categories, even if they guess the right id.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByOwner(currentUserProvider.getCurrentUser())
                .stream().map(CategoryResponse::from).toList();
    }

    @PostMapping
    public CategoryResponse createCategory(@RequestBody CategoryRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();

        Category category = new Category();
        category.setName(request.name());
        category.setMonthlyLimit(request.monthlyLimit());
        category.setOwner(currentUser);
        category.setWallet(resolveWallet(request.walletId(), currentUser));

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to edit this category.");
        }

        category.setName(request.name());
        category.setMonthlyLimit(request.monthlyLimit());
        category.setWallet(resolveWallet(request.walletId(), currentUser));

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getOwner().getId().equals(currentUserProvider.getCurrentUser().getId())) {
            throw new RuntimeException("You don't have permission to delete this category.");
        }

        categoryRepository.deleteById(id);
    }

    private Wallet resolveWallet(Long walletId, User currentUser) {
        if (walletId == null) return null;
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (!wallet.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("This wallet doesn't belong to you.");
        }
        return wallet;
    }
}
