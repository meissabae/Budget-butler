package com.budgetbutler.controller;

import com.budgetbutler.dto.CsvImportResponse;
import com.budgetbutler.dto.PagedResponse;
import com.budgetbutler.dto.TransactionResponse;
import com.budgetbutler.model.Category;
import com.budgetbutler.model.Transaction;
import com.budgetbutler.model.User;
import com.budgetbutler.repository.CategoryRepository;
import com.budgetbutler.repository.TransactionRepository;
import com.budgetbutler.security.CurrentUserProvider;
import com.budgetbutler.service.CsvImportService;
import com.budgetbutler.service.TimeCostService;
import com.budgetbutler.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private TimeCostService timeCostService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private CsvImportService csvImportService;

    /**
     * Paginated by default (page=0, size=20) so a user with thousands of transactions doesn't
     * cause the browser to fetch and render them all at once. The frontend requests more pages
     * as the user clicks "Load more".
     */
    @GetMapping
    public PagedResponse<TransactionResponse> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = currentUserProvider.getCurrentUser();
        Double hourlyRate = timeCostService.getHourlyRate(user); // computed once, reused for every row

        Page<Transaction> resultPage = transactionRepository.findByOwnerOrderByDateDesc(user, PageRequest.of(page, size));
        List<TransactionResponse> content = resultPage.getContent().stream()
                .map(t -> toResponse(t, hourlyRate))
                .toList();

        return new PagedResponse<>(content, page, size, resultPage.getTotalElements(), resultPage.getTotalPages());
    }

    /**
     * Exports EVERY transaction (not just the current page) as a downloadable CSV file -
     * useful for backing up your data or handing it to an accountant/spreadsheet.
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportCsv() {
        User user = currentUserProvider.getCurrentUser();
        List<Transaction> all = transactionRepository.findByOwner(user);

        StringBuilder csv = new StringBuilder("Date,Description,Category,Amount\n");
        for (Transaction t : all) {
            csv.append(escapeCsv(t.getDate().toString())).append(",")
               .append(escapeCsv(t.getDescription())).append(",")
               .append(escapeCsv(t.getCategory() != null ? t.getCategory().getName() : "")).append(",")
               .append(t.getAmount()).append("\n");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment().filename("budget-butler-transactions.csv").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    /** Wraps a value in quotes and escapes any internal quotes, so commas/quotes in descriptions don't break the CSV format. */
    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * Imports expenses from a bank-exported CSV file. Every imported row is assigned to ONE
     * category you choose (banks don't know about our categories) - you can always edit
     * individual transactions afterward to move them to different categories.
     */
    @PostMapping("/import-csv")
    public CsvImportResponse importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("categoryId") Long categoryId) throws IOException {

        User user = currentUserProvider.getCurrentUser();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("This category doesn't belong to you.");
        }

        return csvImportService.importCsv(file, category, user);
    }

    // We accept a small "TransactionRequest" shape from Angular that just references
    // the category by its id, instead of requiring the whole nested Category object.
    @PostMapping
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Extra safety: make sure the category being used actually belongs to this user.
        if (!category.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("This category doesn't belong to you.");
        }

        Transaction transaction = new Transaction(
                request.description(),
                request.amount(),
                request.date(),
                category,
                currentUser
        );
        Transaction saved = transactionRepository.save(transaction);

        // The expense also comes out of the category's wallet, not just its budget limit.
        walletService.debit(category.getWallet(), request.amount());

        Double hourlyRate = timeCostService.getHourlyRate(currentUser);
        return toResponse(saved, hourlyRate);
    }

    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getOwner().getId().equals(currentUserProvider.getCurrentUser().getId())) {
            throw new RuntimeException("You don't have permission to delete this transaction.");
        }

        // Undo the expense's effect on the wallet before removing it.
        if (transaction.getCategory() != null) {
            walletService.credit(transaction.getCategory().getWallet(), transaction.getAmount());
        }

        transactionRepository.deleteById(id);
    }

    /**
     * Editing a transaction is trickier than it looks: the OLD amount already came out of
     * the OLD category's wallet, so we must put that money back first, THEN debit the NEW
     * wallet with the NEW amount - otherwise wallet balances would silently drift wrong
     * every time someone edits an expense.
     */
    @PutMapping("/{id}")
    public TransactionResponse updateTransaction(@PathVariable Long id, @RequestBody TransactionRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to edit this transaction.");
        }

        Category newCategory = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!newCategory.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("This category doesn't belong to you.");
        }

        // Step 1: reverse the OLD transaction's effect on the OLD wallet.
        if (transaction.getCategory() != null) {
            walletService.credit(transaction.getCategory().getWallet(), transaction.getAmount());
        }

        // Step 2: update the transaction with the new values.
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setDate(request.date());
        transaction.setCategory(newCategory);
        Transaction saved = transactionRepository.save(transaction);

        // Step 3: apply the NEW transaction's effect on the NEW wallet.
        walletService.debit(newCategory.getWallet(), request.amount());

        Double hourlyRate = timeCostService.getHourlyRate(currentUser);
        return toResponse(saved, hourlyRate);
    }

    private TransactionResponse toResponse(Transaction t, Double hourlyRate) {
        Double hours = timeCostService.calculateTimeCostHours(t.getAmount(), hourlyRate);
        String message = timeCostService.buildTimeCostMessage(hours);
        return TransactionResponse.from(t, hours, message);
    }

    // A "record" is a quick, simple way in Java to define a small data-holder class.
    // This is the exact shape of JSON the Angular app will POST to us.
    public record TransactionRequest(
            String description,
            Double amount,
            java.time.LocalDate date,
            Long categoryId
    ) {
    }
}
