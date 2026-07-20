package com.budgetbutler.controller;

import com.budgetbutler.dto.DepositRequest;
import com.budgetbutler.dto.WalletRequest;
import com.budgetbutler.dto.WalletResponse;
import com.budgetbutler.dto.WalletUpdateRequest;
import com.budgetbutler.model.User;
import com.budgetbutler.model.Wallet;
import com.budgetbutler.repository.WalletRepository;
import com.budgetbutler.security.CurrentUserProvider;
import com.budgetbutler.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private WalletService walletService;

    @GetMapping
    public List<WalletResponse> getAllWallets() {
        User user = currentUserProvider.getCurrentUser();
        return walletRepository.findByOwner(user).stream().map(WalletResponse::from).toList();
    }

    @PostMapping
    public WalletResponse createWallet(@RequestBody WalletRequest request) {
        Wallet wallet = new Wallet(request.name(), currentUserProvider.getCurrentUser());
        return WalletResponse.from(walletRepository.save(wallet));
    }

    /** Renames a wallet. Balance is never edited directly here - use /deposit to add funds instead. */
    @PutMapping("/{id}")
    public WalletResponse updateWallet(@PathVariable Long id, @RequestBody WalletUpdateRequest request) {
        Wallet wallet = findOwnedWallet(id);
        wallet.setName(request.name());
        return WalletResponse.from(walletRepository.save(wallet));
    }

    @DeleteMapping("/{id}")
    public void deleteWallet(@PathVariable Long id) {
        Wallet wallet = findOwnedWallet(id);
        walletRepository.deleteById(wallet.getId());
    }

    /**
     * Adds money to a wallet anytime - a freelance payment, a gift, cash you deposited,
     * anything that isn't your recurring salary (which is handled automatically instead).
     */
    @PostMapping("/{id}/deposit")
    public WalletResponse deposit(@PathVariable Long id, @RequestBody DepositRequest request) {
        Wallet wallet = findOwnedWallet(id);
        if (request.amount() == null || request.amount() <= 0) {
            throw new RuntimeException("Deposit amount must be greater than 0.");
        }
        walletService.credit(wallet, request.amount());
        return WalletResponse.from(wallet);
    }

    /**
     * Manual "I got paid" button - lets the user credit their salary wallet immediately
     * instead of waiting for the automatic check (which only fires on/after their configured
     * payday). Safe to call more than once - see WalletService for the once-per-month guard.
     */
    @PostMapping("/credit-salary-now")
    public WalletResponse creditSalaryNow() {
        return walletService.creditSalaryNow(currentUserProvider.getCurrentUser());
    }

    private Wallet findOwnedWallet(Long id) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (!wallet.getOwner().getId().equals(currentUserProvider.getCurrentUser().getId())) {
            throw new RuntimeException("You don't have permission to modify this wallet.");
        }
        return wallet;
    }
}
