package com.budgetbutler.repository;

import com.budgetbutler.model.User;
import com.budgetbutler.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByOwner(User owner);
}
