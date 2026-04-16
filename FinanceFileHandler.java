import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistence layer – handles reading and appending transactions to a local
 * {@code finances.csv} file, and reading/appending per-type categories to
 * three separate text files using {@code java.nio.file}.
 *
 * <p><b>Category files (one per transaction type):</b>
 * <ul>
 *   <li>{@code categories_income.txt}     – Income categories</li>
 *   <li>{@code categories_expense.txt}    – Expense categories</li>
 *   <li>{@code categories_investment.txt} – Investment categories</li>
 * </ul>
 * Each file is seeded with sensible defaults on first run if it does not exist.
 */
public class FinanceFileHandler {

    // ── File names ────────────────────────────────────────────────────────────

    private static final String TRANSACTIONS_FILE      = "finances.csv";
    private static final String CATEGORIES_INCOME_FILE = "categories_income.txt";
    private static final String CATEGORIES_EXPENSE_FILE= "categories_expense.txt";
    private static final String CATEGORIES_INVEST_FILE = "categories_investment.txt";

    // ── Default category seeds (one list per type) ────────────────────────────

    private static final List<String> DEFAULT_INCOME = List.of(
        "Salary", "Freelance", "Rental", "Bonus", "Gift", "Other"
    );

    private static final List<String> DEFAULT_EXPENSE = List.of(
        "Food", "Transport", "Market", "Mechanic", "Utilities",
        "Health", "Education", "Entertainment", "Clothing", "Rent",
        "Leisure", "Other"
    );

    private static final List<String> DEFAULT_INVESTMENT = List.of(
        "Fixed Income/CDI", "Stocks", "Crypto", "Real Estate FII",
        "ETF", "Savings", "Other"
    );

    // ── NIO Paths ─────────────────────────────────────────────────────────────

    private final Path transactionsPath;
    private final Path incomeCatsPath;
    private final Path expenseCatsPath;
    private final Path investCatsPath;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a handler and ensures all data files exist.
     * Category files are seeded with type-specific defaults on first run.
     */
    public FinanceFileHandler() {
        this.transactionsPath = Paths.get(TRANSACTIONS_FILE);
        this.incomeCatsPath   = Paths.get(CATEGORIES_INCOME_FILE);
        this.expenseCatsPath  = Paths.get(CATEGORIES_EXPENSE_FILE);
        this.investCatsPath   = Paths.get(CATEGORIES_INVEST_FILE);

        ensureFileExists(transactionsPath, null);          // blank CSV
        ensureFileExists(incomeCatsPath,   DEFAULT_INCOME);
        ensureFileExists(expenseCatsPath,  DEFAULT_EXPENSE);
        ensureFileExists(investCatsPath,   DEFAULT_INVESTMENT);
    }

    // ── File initialisation ───────────────────────────────────────────────────

    /**
     * Creates {@code path} if it does not already exist.
     * If {@code defaults} is non-null the file is seeded with those lines;
     * otherwise an empty file is created.
     */
    private void ensureFileExists(Path path, List<String> defaults) {
        if (Files.exists(path)) return;
        try {
            if (defaults == null || defaults.isEmpty()) {
                Files.createFile(path);
            } else {
                StringBuilder sb = new StringBuilder();
                for (String line : defaults) {
                    sb.append(line).append(System.lineSeparator());
                }
                Files.writeString(path, sb.toString(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
            System.out.println("[FinanceFileHandler] Created: " + path.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[FinanceFileHandler] Could not create " + path + ": " + e.getMessage());
        }
    }

    // ── Resolve category path from Type ──────────────────────────────────────

    /**
     * Returns the correct category file {@link Path} for the given transaction type.
     *
     * @param type the transaction type (must not be {@code null})
     * @return the corresponding category file path
     */
    private Path categoryPathFor(TransactionEntry.Type type) {
        return switch (type) {
            case INCOME     -> incomeCatsPath;
            case EXPENSE    -> expenseCatsPath;
            case INVESTMENT -> investCatsPath;
        };
    }

    // ── Transaction Read / Write ──────────────────────────────────────────────

    /**
     * Reads all transactions from the CSV file.
     * Malformed rows are skipped with a stderr warning.
     *
     * @return unmodifiable list of {@link TransactionEntry} objects
     */
    public List<TransactionEntry> getAllTransactions() {
        List<TransactionEntry> entries = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(transactionsPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                TransactionEntry entry = TransactionEntry.fromCsv(line);
                if (entry != null) entries.add(entry);
            }
        } catch (IOException e) {
            System.err.println("[FinanceFileHandler] Error reading transactions: " + e.getMessage());
        }
        return Collections.unmodifiableList(entries);
    }

    /**
     * Appends a single transaction as a new CSV row.
     *
     * @param entry the transaction to persist (must not be {@code null})
     * @return {@code true} on success, {@code false} on I/O failure
     */
    public boolean appendTransaction(TransactionEntry entry) {
        if (entry == null) throw new IllegalArgumentException("Entry must not be null.");
        try {
            Files.writeString(transactionsPath,
                    entry.toCsv() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
            return true;
        } catch (IOException e) {
            System.err.println("[FinanceFileHandler] Error writing transaction: " + e.getMessage());
            return false;
        }
    }

    // ── Category Read / Write ─────────────────────────────────────────────────

    /**
     * Reads all categories for the given transaction type from its dedicated file.
     * Blank lines are silently skipped.
     *
     * @param type the transaction type whose categories to load
     * @return mutable list of category strings; never {@code null}
     */
    public List<String> getCategoriesFor(TransactionEntry.Type type) {
        List<String> categories = new ArrayList<>();
        Path path = categoryPathFor(type);
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) categories.add(trimmed);
            }
        } catch (IOException e) {
            System.err.println("[FinanceFileHandler] Error reading categories for "
                    + type + ": " + e.getMessage());
        }
        // Safety fallback so the combo is never empty
        if (categories.isEmpty()) categories.add("Other");
        return categories;
    }

    /**
     * Appends a new category to the file that belongs to {@code type}.
     * Performs a case-insensitive duplicate check before writing.
     *
     * @param type         the transaction type this category belongs to
     * @param categoryName the category name to add
     * @return {@code true} if written, {@code false} if duplicate or I/O error
     * @throws IllegalArgumentException if {@code categoryName} is blank
     */
    public boolean appendCategory(TransactionEntry.Type type, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank.");
        }

        // Duplicate guard (case-insensitive, scoped to this type's list)
        boolean duplicate = getCategoriesFor(type).stream()
                .anyMatch(c -> c.equalsIgnoreCase(categoryName.trim()));
        if (duplicate) {
            System.out.println("[FinanceFileHandler] Duplicate category for "
                    + type + ": " + categoryName);
            return false;
        }

        try {
            Files.writeString(categoryPathFor(type),
                    categoryName.trim() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
            return true;
        } catch (IOException e) {
            System.err.println("[FinanceFileHandler] Error writing category: " + e.getMessage());
            return false;
        }
    }
}
