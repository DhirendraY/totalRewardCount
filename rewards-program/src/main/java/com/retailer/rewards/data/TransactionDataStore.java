package com.retailer.rewards.data;

import com.retailer.rewards.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * In-memory data store that seeds sample transactions for demonstration purposes.
 *
 * <p>This component pre-populates five customers with varied transaction amounts
 * and dates spread across multiple calendar months so that every reward-point
 * tier (below $50, between $50–$100, and above $100) is exercised.</p>
 *
 * <p>In a production system this class would be replaced by a database
 * repository, but for this assignment it removes the need for any external
 * infrastructure.</p>
 */
@Component
public class TransactionDataStore {

    /** The immutable, pre-seeded list of all transactions. */
    private final List<Transaction> transactions;

    /**
     * Constructs the data store and seeds it with sample transactions.
     *
     * <p>Transactions span three months relative to a reference date so that
     * the reward calculations are always spread across multiple months
     * regardless of when the application is started.</p>
     */
    public TransactionDataStore() {
        // Anchor: today's date – transactions are seeded relative to this.
        LocalDate today = LocalDate.now();
        LocalDate m1 = today.minusMonths(2).withDayOfMonth(5);   // two months ago
        LocalDate m2 = today.minusMonths(1).withDayOfMonth(10);  // last month
        LocalDate m3 = today.withDayOfMonth(3);                  // current month

        transactions = List.of(

                // ── Customer C001 – Alice Smith ───────────────────────────────────
                txn("C001", "Alice Smith",  new BigDecimal("120.00"), m1),   // 90 pts
                txn("C001", "Alice Smith",  new BigDecimal("75.50"),  m1),   // 25 pts
                txn("C001", "Alice Smith",  new BigDecimal("200.00"), m2),   // 250 pts
                txn("C001", "Alice Smith",  new BigDecimal("45.00"),  m2),   // 0 pts  (< $50)
                txn("C001", "Alice Smith",  new BigDecimal("110.00"), m3),   // 70 pts

                // ── Customer C002 – Bob Johnson ───────────────────────────────────
                txn("C002", "Bob Johnson",  new BigDecimal("85.00"),  m1),   // 35 pts
                txn("C002", "Bob Johnson",  new BigDecimal("30.00"),  m1),   // 0 pts  (< $50)
                txn("C002", "Bob Johnson",  new BigDecimal("150.00"), m2),   // 150 pts
                txn("C002", "Bob Johnson",  new BigDecimal("99.99"),  m2),   // 49 pts
                txn("C002", "Bob Johnson",  new BigDecimal("60.00"),  m3),   // 10 pts
                txn("C002", "Bob Johnson",  new BigDecimal("250.00"), m3),   // 350 pts

                // ── Customer C003 – Carol Williams ────────────────────────────────
                txn("C003", "Carol Williams", new BigDecimal("500.00"), m1), // 850 pts
                txn("C003", "Carol Williams", new BigDecimal("50.00"),  m1), // 0 pts  (exactly $50)
                txn("C003", "Carol Williams", new BigDecimal("51.00"),  m2), // 1 pt
                txn("C003", "Carol Williams", new BigDecimal("100.00"), m2), // 50 pts (exactly $100)
                txn("C003", "Carol Williams", new BigDecimal("101.00"), m3), // 52 pts

                // ── Customer C004 – David Brown ───────────────────────────────────
                txn("C004", "David Brown",  new BigDecimal("49.99"),  m1),   // 0 pts
                txn("C004", "David Brown",  new BigDecimal("75.00"),  m2),   // 25 pts
                txn("C004", "David Brown",  new BigDecimal("130.00"), m2),   // 110 pts
                txn("C004", "David Brown",  new BigDecimal("90.00"),  m3),   // 40 pts

                // ── Customer C005 – Eva Martinez ─────────────────────────────────
                txn("C005", "Eva Martinez", new BigDecimal("1000.00"), m1),  // 1850 pts
                txn("C005", "Eva Martinez", new BigDecimal("55.00"),   m2),  // 5 pts
                txn("C005", "Eva Martinez", new BigDecimal("0.99"),    m2),  // 0 pts  (< $1)
                txn("C005", "Eva Martinez", new BigDecimal("100.01"),  m3)   // 50 pts (just over $100)
        );
    }

    /**
     * Returns the full list of pre-seeded transactions.
     *
     * @return an unmodifiable list of all {@link Transaction} records
     */
    public List<Transaction> getAllTransactions() {
        return transactions;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Convenience factory for building a {@link Transaction} with a generated ID.
     *
     * @param customerId   the customer identifier
     * @param customerName the customer display name
     * @param amount       the purchase amount
     * @param date         the date of the purchase
     * @return a fully populated {@link Transaction}
     */
    private Transaction txn(String customerId, String customerName,
                             BigDecimal amount, LocalDate date) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .customerId(customerId)
                .customerName(customerName)
                .amount(amount)
                .transactionDate(date)
                .build();
    }
}
