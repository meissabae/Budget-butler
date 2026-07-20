package com.budgetbutler.service;

import com.budgetbutler.dto.CsvImportResponse;
import com.budgetbutler.model.Category;
import com.budgetbutler.model.Transaction;
import com.budgetbutler.model.User;
import com.budgetbutler.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every bank exports CSV slightly differently - different column names, different date
 * formats, sometimes separate "Debit"/"Credit" columns instead of one signed "Amount" column.
 * Rather than support every bank perfectly, this takes a pragmatic approach: look for
 * recognizable column names (case-insensitive), try a handful of common date formats, and
 * skip (with a clear reason) any row it can't confidently parse - so a partially-messy file
 * still imports the rows it CAN understand instead of failing the whole thing.
 */
@Service
public class CsvImportService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    private static final List<String> DATE_COLUMN_NAMES = List.of("date", "transaction date", "posted date", "trans date");
    private static final List<String> DESCRIPTION_COLUMN_NAMES = List.of("description", "memo", "details", "narrative", "payee");
    private static final List<String> AMOUNT_COLUMN_NAMES = List.of("amount", "value");
    private static final List<String> DEBIT_COLUMN_NAMES = List.of("debit", "withdrawal", "money out");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,               // 2026-07-15
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    );

    public CsvImportResponse importCsv(MultipartFile file, Category category, User user) throws IOException {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new CsvImportResponse(0, 0, List.of("The file is empty."));
            }

            List<String> headers = parseCsvLine(headerLine);
            int dateCol = findColumn(headers, DATE_COLUMN_NAMES);
            int descriptionCol = findColumn(headers, DESCRIPTION_COLUMN_NAMES);
            int amountCol = findColumn(headers, AMOUNT_COLUMN_NAMES);
            int debitCol = amountCol == -1 ? findColumn(headers, DEBIT_COLUMN_NAMES) : -1;

            if (dateCol == -1 || descriptionCol == -1 || (amountCol == -1 && debitCol == -1)) {
                return new CsvImportResponse(0, 0, List.of(
                        "Couldn't find recognizable Date/Description/Amount columns in the header row. " +
                        "Expected something like: Date, Description, Amount"));
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) continue;

                List<String> fields = parseCsvLine(line);
                int neededCols = Math.max(dateCol, Math.max(descriptionCol, Math.max(amountCol, debitCol))) + 1;
                if (fields.size() < neededCols) {
                    skipped++;
                    errors.add("Row " + rowNumber + ": not enough columns, skipped.");
                    continue;
                }

                LocalDate date = parseDate(fields.get(dateCol));
                if (date == null) {
                    skipped++;
                    errors.add("Row " + rowNumber + ": couldn't understand the date '" + fields.get(dateCol) + "', skipped.");
                    continue;
                }

                Double amount = parseAmount(amountCol != -1 ? fields.get(amountCol) : fields.get(debitCol));
                if (amount == null || amount == 0) {
                    skipped++;
                    errors.add("Row " + rowNumber + ": couldn't understand the amount, skipped.");
                    continue;
                }

                // Bank exports often show expenses as negative numbers - we only care about
                // the expense magnitude here, since Budget Butler tracks spending, not income.
                amount = Math.abs(amount);

                String description = fields.get(descriptionCol).trim();
                if (description.isEmpty()) description = "Imported expense";

                Transaction transaction = new Transaction(description, amount, date, category, user);
                transactionRepository.save(transaction);
                walletService.debit(category.getWallet(), amount);
                imported++;
            }
        }

        // Cap the error list so a huge messy file doesn't return a massive response.
        List<String> trimmedErrors = errors.size() > 20 ? errors.subList(0, 20) : errors;
        return new CsvImportResponse(imported, skipped, trimmedErrors);
    }

    private int findColumn(List<String> headers, List<String> candidateNames) {
        for (int i = 0; i < headers.size(); i++) {
            String normalized = headers.get(i).trim().toLowerCase(Locale.ROOT);
            if (candidateNames.contains(normalized)) {
                return i;
            }
        }
        return -1;
    }

    private LocalDate parseDate(String raw) {
        String trimmed = raw.trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    private Double parseAmount(String raw) {
        try {
            // Strip currency symbols, commas, and surrounding whitespace/quotes banks often add.
            String cleaned = raw.replaceAll("[^0-9.\\-]", "");
            if (cleaned.isEmpty() || cleaned.equals("-")) return null;
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A small hand-rolled CSV line splitter that respects quoted fields containing commas. */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
