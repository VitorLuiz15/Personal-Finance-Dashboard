import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Service layer responsible for business logic and financial calculations.
 * Centralizes data aggregation, CDI yield simulations, and cashback logic.
 */
public class FinanceService {

    private final FinanceFileHandler fileHandler;

    /**
     * Constructs a FinanceService with a given file handler.
     * @param fileHandler the persistence handler to fetch raw data
     */
    public FinanceService(FinanceFileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    /**
     * Calculates the sum of all income transactions.
     * @return total income amount
     */
    public double getTotalIncome() {
        return fileHandler.getAllTransactions().stream()
                .filter(e -> e.getType() == TransactionEntry.Type.INCOME)
                .mapToDouble(TransactionEntry::getAmount).sum();
    }

    /**
     * Calculates the sum of all expense transactions.
     * @return total expense amount
     */
    public double getTotalExpenses() {
        return fileHandler.getAllTransactions().stream()
                .filter(e -> e.getType() == TransactionEntry.Type.EXPENSE)
                .mapToDouble(TransactionEntry::getAmount).sum();
    }

    /**
     * Calculates the sum of all investment transactions.
     * @return total invested amount
     */
    public double getTotalInvestments() {
        return fileHandler.getAllTransactions().stream()
                .filter(e -> e.getType() == TransactionEntry.Type.INVESTMENT)
                .mapToDouble(TransactionEntry::getAmount).sum();
    }

    /**
     * Calculates the net balance: Income - (Expenses + Investments).
     * @return final available balance
     */
    public double getNetBalance() {
        return getTotalIncome() - getTotalExpenses() - getTotalInvestments();
    }

    /**
     * Calculates estimated cashback for a given total expense.
     * @param expenseAmount the base value for calculation
     * @param percentage the cashback rate (e.g., 0.01 for 1%)
     * @return calculated cashback value
     */
    public double calculateCashback(double expenseAmount, double percentage) {
        return expenseAmount * percentage;
    }

    /**
     * Simulates CDI yield using compound interest (pro-rata).
     * Formula: M = P * (1 + i)^n
     * 
     * @param principal the initial amount invested
     * @param annualRate the annual interest rate (e.g., 0.1175 for 11.75% SELIC/CDI)
     * @param days the investment period in days
     * @return the total amount including yields
     */
    public double calculateCDIYield(double principal, double annualRate, int days) {
        // Daily rate derived from annual rate (360-day banking year assumption)
        double dailyRate = Math.pow(1 + annualRate, 1.0 / 360.0) - 1;
        return principal * Math.pow(1 + dailyRate, days);
    }

    /**
     * Aggregates expenses by category for chart visualization.
     * @return map of category names to their respective total amounts
     */
    public Map<String, Double> getExpensesByCategory() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (TransactionEntry e : fileHandler.getAllTransactions()) {
            if (e.getType() == TransactionEntry.Type.EXPENSE) {
                map.merge(e.getCategory(), e.getAmount(), Double::sum);
            }
        }
        return map;
    }
}
