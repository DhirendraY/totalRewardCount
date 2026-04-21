package com.retailer.rewards.service;

import com.retailer.rewards.data.TransactionDataStore;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.model.CustomerRewards;
import com.retailer.rewards.model.MonthlyRewards;
import com.retailer.rewards.model.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core business-logic service for the Retailer Rewards Program.
 *
 * <p>This service is responsible for:
 * <ol>
 *   <li>Calculating the reward points earned for a single transaction amount.</li>
 *   <li>Aggregating per-customer reward totals broken down by calendar month.</li>
 *   <li>Filtering transactions by an optional date range.</li>
 * </ol>
 *
 * <p><strong>Reward rules:</strong>
 * <ul>
 *   <li>Transactions below $50 earn <strong>0</strong> points.</li>
 *   <li>Transactions between $50 (exclusive) and $100 (inclusive) earn
 *       <strong>1 point per dollar</strong> for the portion above $50.</li>
 *   <li>Transactions above $100 earn <strong>1 point per dollar</strong>
 *       for the portion between $50 and $100, plus
 *       <strong>2 points per dollar</strong> for the portion above $100.</li>
 * </ul>
 */
@Service
public class RewardsService {

    /** Lower spend threshold – no points earned below or at this amount. */
    private static final BigDecimal LOWER_THRESHOLD = new BigDecimal("50");

    /** Upper spend threshold – 2× points apply above this amount. */
    private static final BigDecimal UPPER_THRESHOLD = new BigDecimal("100");

    private final TransactionDataStore dataStore;

    /**
     * Constructs the service with the injected {@link TransactionDataStore}.
     *
     * @param dataStore the data source providing all transactions
     */
    public RewardsService(TransactionDataStore dataStore) {
        this.dataStore = dataStore;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Calculates reward points for every customer across all recorded transactions.
     *
     * @return a list of {@link CustomerRewards}, one per customer, sorted by customer ID
     */
    public List<CustomerRewards> getAllCustomerRewards() {
        return computeRewards(dataStore.getAllTransactions());
    }

    /**
     * Calculates reward points for a specific customer across all their transactions.
     *
     * @param customerId the customer identifier to look up
     * @return the {@link CustomerRewards} for the requested customer
     * @throws CustomerNotFoundException if no transactions exist for the given customer ID
     */
    public CustomerRewards getRewardsByCustomerId(String customerId) {
        List<Transaction> customerTransactions = dataStore.getAllTransactions().stream()
                .filter(t -> t.getCustomerId().equalsIgnoreCase(customerId))
                .toList();

        if (customerTransactions.isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }

        return computeRewards(customerTransactions).get(0);
    }

    /**
     * Calculates reward points for every customer within a specified date range.
     *
     * @param startDate inclusive start date of the period
     * @param endDate   inclusive end date of the period
     * @return a list of {@link CustomerRewards} filtered to the given period
     * @throws InvalidDateRangeException if {@code startDate} is after {@code endDate}
     *                                   or if either date is {@code null}
     */
    public List<CustomerRewards> getRewardsByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<Transaction> filtered = dataStore.getAllTransactions().stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate)
                          && !t.getTransactionDate().isAfter(endDate))
                .toList();

        return computeRewards(filtered);
    }

    /**
     * Calculates reward points for a specific customer within a specified date range.
     *
     * @param customerId the customer identifier to look up
     * @param startDate  inclusive start date of the period
     * @param endDate    inclusive end date of the period
     * @return the {@link CustomerRewards} for the requested customer in the given period
     * @throws InvalidDateRangeException if the date range is invalid
     * @throws CustomerNotFoundException if no transactions exist for the customer in the range
     */
    public CustomerRewards getRewardsByCustomerAndDateRange(String customerId,
                                                            LocalDate startDate,
                                                            LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<Transaction> filtered = dataStore.getAllTransactions().stream()
                .filter(t -> t.getCustomerId().equalsIgnoreCase(customerId)
                          && !t.getTransactionDate().isBefore(startDate)
                          && !t.getTransactionDate().isAfter(endDate))
                .toList();

        if (filtered.isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }

        return computeRewards(filtered).get(0);
    }

    /**
     * Calculates the reward points earned for a single transaction amount.
     *
     * <p>The calculation uses integer (floor) arithmetic on the dollar portion:</p>
     * <ul>
     *   <li>Amount ≤ $50 → 0 points</li>
     *   <li>$50 &lt; amount ≤ $100 → {@code floor(amount) - 50} points</li>
     *   <li>Amount &gt; $100 → {@code (floor(amount) - 100) × 2 + 50} points</li>
     * </ul>
     *
     * @param amount the purchase amount; must not be {@code null}
     * @return the reward points earned (always ≥ 0)
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    public long calculatePoints(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transaction amount must not be negative");
        }

        long dollars = amount.longValue(); // floor to whole dollars

        if (dollars <= LOWER_THRESHOLD.longValue()) {
            return 0L;
        } else if (dollars <= UPPER_THRESHOLD.longValue()) {
            return dollars - LOWER_THRESHOLD.longValue();
        } else {
            long aboveHundred  = dollars - UPPER_THRESHOLD.longValue();
            long fiftyToHundred = UPPER_THRESHOLD.longValue() - LOWER_THRESHOLD.longValue();
            return (aboveHundred * 2) + fiftyToHundred;
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Groups transactions by customer then by year-month, computes points for
     * each transaction, and builds the aggregated {@link CustomerRewards} list.
     *
     * @param transactions the transactions to process
     * @return sorted list of {@link CustomerRewards} (by customer ID)
     */
    private List<CustomerRewards> computeRewards(List<Transaction> transactions) {
        // Group by customer ID
        Map<String, List<Transaction>> byCustomer = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCustomerId));

        return byCustomer.entrySet().stream()
                .map(entry -> buildCustomerRewards(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CustomerRewards::getCustomerId))
                .collect(Collectors.toList());
    }

    /**
     * Builds a {@link CustomerRewards} object for one customer by aggregating
     * their transactions month by month.
     *
     * @param customerId   the customer identifier
     * @param transactions all transactions belonging to this customer
     * @return the completed {@link CustomerRewards}
     */
    private CustomerRewards buildCustomerRewards(String customerId, List<Transaction> transactions) {
        String customerName = transactions.get(0).getCustomerName();

        // Group transactions by YearMonth
        Map<YearMonth, List<Transaction>> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getTransactionDate())));

        List<MonthlyRewards> monthlyRewards = byMonth.entrySet().stream()
                .map(entry -> {
                    YearMonth ym = entry.getKey();
                    long monthPoints = entry.getValue().stream()
                            .mapToLong(t -> calculatePoints(t.getAmount()))
                            .sum();
                    return MonthlyRewards.builder()
                            .year(ym.getYear())
                            .month(ym.getMonthValue())
                            .monthLabel(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase()
                                    + " " + ym.getYear())
                            .points(monthPoints)
                            .build();
                })
                .sorted(Comparator.comparing(mr -> YearMonth.of(mr.getYear(), mr.getMonth())))
                .collect(Collectors.toList());

        long total = monthlyRewards.stream().mapToLong(MonthlyRewards::getPoints).sum();

        return CustomerRewards.builder()
                .customerId(customerId)
                .customerName(customerName)
                .monthlyRewards(monthlyRewards)
                .totalPoints(total)
                .build();
    }

    /**
     * Validates that the provided date range is logically consistent.
     *
     * @param startDate the start of the range
     * @param endDate   the end of the range
     * @throws InvalidDateRangeException if either date is {@code null} or
     *                                   {@code startDate} is after {@code endDate}
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidDateRangeException("Both startDate and endDate must be provided");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException(
                    "startDate (" + startDate + ") must not be after endDate (" + endDate + ")");
        }
    }
}
