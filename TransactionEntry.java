import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Model class representing a single financial transaction.
 * Supports serialization/deserialization to/from CSV format.
 */
public class TransactionEntry {

    // ── Enumerations ──────────────────────────────────────────────────────────

    public enum Type {
        INCOME, EXPENSE, INVESTMENT;

        /** Human-readable label used in the UI and CSV. */
        public String label() {
            return switch (this) {
                case INCOME     -> "Income";
                case EXPENSE    -> "Expense";
                case INVESTMENT -> "Investment";
            };
        }

        public static Type fromLabel(String label) {
            for (Type t : values()) {
                if (t.label().equalsIgnoreCase(label.trim())) return t;
            }
            throw new IllegalArgumentException("Unknown type: " + label);
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Formatter used for CSV date column. */
    private static final DateTimeFormatter CSV_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Column delimiter for CSV rows. */
    private static final String DELIMITER = ";";

    // ── Fields ────────────────────────────────────────────────────────────────

    private Type          type;
    private String        category;
    private double        amount;
    private LocalDateTime dateTime;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor – used when creating a new entry from the form.
     *
     * @param type     transaction type (Income / Expense / Investment)
     * @param category user-selected category string
     * @param amount   monetary value (always positive)
     * @param dateTime timestamp of the transaction
     */
    public TransactionEntry(Type type, String category, double amount, LocalDateTime dateTime) {
        this.type     = type;
        this.category = category;
        this.amount   = amount;
        this.dateTime = dateTime;
    }

    /** Convenience constructor that defaults dateTime to now. */
    public TransactionEntry(Type type, String category, double amount) {
        this(type, category, amount, LocalDateTime.now());
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Type          getType()     { return type;     }
    public String        getCategory() { return category; }
    public double        getAmount()   { return amount;   }
    public LocalDateTime getDateTime() { return dateTime; }

    public void setType(Type type)          { this.type     = type;     }
    public void setCategory(String cat)     { this.category = cat;      }
    public void setAmount(double amount)    { this.amount   = amount;   }
    public void setDateTime(LocalDateTime d){ this.dateTime = d;        }

    // ── CSV Serialization ─────────────────────────────────────────────────────

    /**
     * Serializes this entry to a single CSV line.
     * Format: TYPE;CATEGORY;AMOUNT;DATE_TIME
     *
     * @return CSV-formatted string (no trailing newline)
     */
    public String toCsv() {
        return String.join(DELIMITER,
                type.label(),
                category,
                String.valueOf(amount),
                dateTime.format(CSV_FORMATTER));
    }

    /**
     * Deserializes a CSV line produced by {@link #toCsv()} back into a
     * {@code TransactionEntry}. Returns {@code null} if the line is malformed
     * so that callers can safely skip corrupt rows.
     *
     * @param line raw CSV line (may contain leading/trailing whitespace)
     * @return parsed entry or {@code null} on parse failure
     */
    public static TransactionEntry fromCsv(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split(DELIMITER, -1);
        if (parts.length < 4) return null;

        try {
            Type          type     = Type.fromLabel(parts[0].trim());
            String        category = parts[1].trim();
            double        amount   = Double.parseDouble(parts[2].trim());
            LocalDateTime dt       = LocalDateTime.parse(parts[3].trim(), CSV_FORMATTER);
            return new TransactionEntry(type, category, amount, dt);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            System.err.println("[TransactionEntry] Skipping malformed CSV row: " + line);
            return null;
        }
    }

    // ── Object Overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("TransactionEntry{type=%s, category='%s', amount=%.2f, dateTime=%s}",
                type.label(), category, amount, dateTime.format(CSV_FORMATTER));
    }
}
