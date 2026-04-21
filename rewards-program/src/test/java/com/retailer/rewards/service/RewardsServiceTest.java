package com.retailer.rewards.service;

import com.retailer.rewards.data.TransactionDataStore;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.model.CustomerRewards;
import com.retailer.rewards.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link RewardsService}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Point calculation edge cases (boundaries, negatives, null input).</li>
 *   <li>Per-customer and all-customer reward aggregation.</li>
 *   <li>Date-range filtering including invalid ranges.</li>
 *   <li>Exception scenarios (unknown customer, bad dates, negative amounts).</li>
 * </ul>
 * </p>
 */
class RewardsServiceTest {

    private RewardsService rewardsService;

    @BeforeEach
    void setUp() {
        rewardsService = new RewardsService(new TransactionDataStore());
    }

    // =========================================================================
    // calculatePoints – boundary and typical values
    // =========================================================================

    @Nested
    @DisplayName("calculatePoints()")
    class CalculatePointsTests {

        @ParameterizedTest(name = "amount=${0} → {1} points")
        @CsvSource({
                "0.00,   0",    // zero spend
                "10.00,  0",    // below lower threshold
                "49.99,  0",    // just below lower threshold
                "50.00,  0",    // exactly at lower threshold – no points
                "50.01,  0",    // just above lower threshold (floor = 50)
                "51.00,  1",    // 1 dollar into the middle tier
                "75.00,  25",   // mid-range: 75-50 = 25
                "100.00, 50",   // exactly at upper threshold: 100-50 = 50
                "100.01, 50",   // just over $100 (floor = 100, same as $100)
                "101.00, 52",   // 1 dollar into the top tier: 50 + 2*1
                "120.00, 90",   // example from spec: 2*20 + 1*50 = 90
                "200.00, 250",  // 2*100 + 50 = 250
                "500.00, 850",  // 2*400 + 50 = 850
                "1000.00,1850"  // 2*900 + 50 = 1850
        })
        @DisplayName("Parameterised point calculations")
        void shouldCalculateCorrectPoints(BigDecimal amount, long expectedPoints) {
            assertThat(rewardsService.calculatePoints(amount)).isEqualTo(expectedPoints);
        }

        @Test
        @DisplayName("Null amount throws IllegalArgumentException")
        void nullAmountShouldThrow() {
            assertThatThrownBy(() -> rewardsService.calculatePoints(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("Negative amount throws IllegalArgumentException")
        void negativeAmountShouldThrow() {
            assertThatThrownBy(() -> rewardsService.calculatePoints(new BigDecimal("-1.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }
    }

    // =========================================================================
    // getAllCustomerRewards
    // =========================================================================

    @Nested
    @DisplayName("getAllCustomerRewards()")
    class GetAllCustomerRewardsTests {

        @Test
        @DisplayName("Returns rewards for all five seeded customers")
        void shouldReturnAllCustomers() {
            List<CustomerRewards> result = rewardsService.getAllCustomerRewards();
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("Results are sorted by customer ID")
        void shouldBeSortedByCustomerId() {
            List<String> ids = rewardsService.getAllCustomerRewards().stream()
                    .map(CustomerRewards::getCustomerId)
                    .toList();
            assertThat(ids).isSorted();
        }

        @Test
        @DisplayName("Total points are non-negative for every customer")
        void totalPointsShouldBeNonNegative() {
            rewardsService.getAllCustomerRewards()
                    .forEach(cr -> assertThat(cr.getTotalPoints()).isGreaterThanOrEqualTo(0));
        }

        @Test
        @DisplayName("Monthly rewards list is not empty for every customer")
        void monthlyRewardsShouldNotBeEmpty() {
            rewardsService.getAllCustomerRewards()
                    .forEach(cr -> assertThat(cr.getMonthlyRewards()).isNotEmpty());
        }
    }

    // =========================================================================
    // getRewardsByCustomerId
    // =========================================================================

    @Nested
    @DisplayName("getRewardsByCustomerId()")
    class GetRewardsByCustomerIdTests {

        @Test
        @DisplayName("Returns correct customer name for C001")
        void shouldReturnCorrectCustomerName() {
            CustomerRewards rewards = rewardsService.getRewardsByCustomerId("C001");
            assertThat(rewards.getCustomerName()).isEqualTo("Alice Smith");
        }

        @Test
        @DisplayName("Total points for C001 match manually calculated value")
        void shouldCalculateCorrectTotalForAlice() {
            // Seeded: 90 + 25 + 250 + 0 + 70 = 435
            CustomerRewards rewards = rewardsService.getRewardsByCustomerId("C001");
            assertThat(rewards.getTotalPoints()).isEqualTo(435L);
        }

        @Test
        @DisplayName("Total points for C005 (Eva – $1000 purchase) are correct")
        void shouldCalculateCorrectTotalForEva() {
            // Seeded: 1850 + 5 + 0 + 50 = 1905
            CustomerRewards rewards = rewardsService.getRewardsByCustomerId("C005");
            assertThat(rewards.getTotalPoints()).isEqualTo(1905L);
        }

        @Test
        @DisplayName("Lookup is case-insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(rewardsService.getRewardsByCustomerId("c001"))
                    .isNotNull()
                    .extracting(CustomerRewards::getCustomerId)
                    .isEqualTo("C001");
        }

        @Test
        @DisplayName("Unknown customer ID throws CustomerNotFoundException")
        void unknownCustomerShouldThrow() {
            assertThatThrownBy(() -> rewardsService.getRewardsByCustomerId("UNKNOWN"))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("UNKNOWN");
        }

        @Test
        @DisplayName("Monthly rewards are sorted chronologically")
        void monthlyShouldBeChronological() {
            CustomerRewards rewards = rewardsService.getRewardsByCustomerId("C001");
            List<String> labels = rewards.getMonthlyRewards().stream()
                    .map(mr -> mr.getYear() + "-" + String.format("%02d", mr.getMonth()))
                    .toList();
            assertThat(labels).isSorted();
        }
    }

    // =========================================================================
    // getRewardsByDateRange
    // =========================================================================

    @Nested
    @DisplayName("getRewardsByDateRange()")
    class GetRewardsByDateRangeTests {

        @Test
        @DisplayName("Very narrow range (single day) returns only matching transactions")
        void singleDayRangeShouldWork() {
            // Use a range far in the future – expect no transactions
            LocalDate future = LocalDate.now().plusYears(5);
            List<CustomerRewards> result = rewardsService.getRewardsByDateRange(future, future);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Full historical range returns all customers")
        void fullRangeShouldReturnAllCustomers() {
            LocalDate start = LocalDate.now().minusYears(1);
            LocalDate end   = LocalDate.now().plusDays(1);
            assertThat(rewardsService.getRewardsByDateRange(start, end)).hasSize(5);
        }

        @Test
        @DisplayName("startDate after endDate throws InvalidDateRangeException")
        void invertedRangeShouldThrow() {
            LocalDate start = LocalDate.now();
            LocalDate end   = start.minusDays(1);
            assertThatThrownBy(() -> rewardsService.getRewardsByDateRange(start, end))
                    .isInstanceOf(InvalidDateRangeException.class)
                    .hasMessageContaining("startDate");
        }

        @Test
        @DisplayName("Null startDate throws InvalidDateRangeException")
        void nullStartDateShouldThrow() {
            assertThatThrownBy(() -> rewardsService.getRewardsByDateRange(null, LocalDate.now()))
                    .isInstanceOf(InvalidDateRangeException.class);
        }

        @Test
        @DisplayName("Null endDate throws InvalidDateRangeException")
        void nullEndDateShouldThrow() {
            assertThatThrownBy(() -> rewardsService.getRewardsByDateRange(LocalDate.now(), null))
                    .isInstanceOf(InvalidDateRangeException.class);
        }

        @Test
        @DisplayName("Same start and end date is a valid range")
        void sameDateRangeShouldNotThrow() {
            LocalDate today = LocalDate.now();
            // Should not throw – result may be empty but no exception expected
            assertThatCode(() -> rewardsService.getRewardsByDateRange(today, today))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // getRewardsByCustomerAndDateRange
    // =========================================================================

    @Nested
    @DisplayName("getRewardsByCustomerAndDateRange()")
    class GetRewardsByCustomerAndDateRangeTests {

        @Test
        @DisplayName("Returns correct points when customer has transactions in range")
        void shouldReturnCorrectPointsInRange() {
            LocalDate start = LocalDate.now().minusMonths(3);
            LocalDate end   = LocalDate.now();
            CustomerRewards rewards =
                    rewardsService.getRewardsByCustomerAndDateRange("C002", start, end);
            assertThat(rewards.getTotalPoints()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Throws CustomerNotFoundException for valid range but wrong ID")
        void unknownCustomerInRangeShouldThrow() {
            LocalDate start = LocalDate.now().minusMonths(3);
            LocalDate end   = LocalDate.now();
            assertThatThrownBy(() ->
                    rewardsService.getRewardsByCustomerAndDateRange("C999", start, end))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("Throws CustomerNotFoundException when valid customer has no transactions in range")
        void noTransactionsInRangeShouldThrow() {
            LocalDate start = LocalDate.of(2000, 1, 1);
            LocalDate end   = LocalDate.of(2000, 3, 31);
            assertThatThrownBy(() ->
                    rewardsService.getRewardsByCustomerAndDateRange("C001", start, end))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("Throws InvalidDateRangeException for inverted dates")
        void invertedRangeShouldThrow() {
            assertThatThrownBy(() ->
                    rewardsService.getRewardsByCustomerAndDateRange(
                            "C001", LocalDate.now(), LocalDate.now().minusDays(1)))
                    .isInstanceOf(InvalidDateRangeException.class);
        }
    }

    // =========================================================================
    // Additional edge cases with custom transaction sets
    // =========================================================================

    @Nested
    @DisplayName("Custom transaction edge cases")
    class CustomTransactionEdgeCases {

        /**
         * Wraps the service around a custom data set for isolated point-calculation checks.
         */
        private CustomerRewards rewardsForAmount(BigDecimal amount) {
            // Build a minimal TransactionDataStore substitute inline
            TransactionDataStore store = new TransactionDataStore() {
                @Override
                public List<Transaction> getAllTransactions() {
                    return List.of(Transaction.builder()
                            .transactionId("T1")
                            .customerId("TEST")
                            .customerName("Test Customer")
                            .amount(amount)
                            .transactionDate(LocalDate.now())
                            .build());
                }
            };
            return new RewardsService(store).getRewardsByCustomerId("TEST");
        }

        @Test
        @DisplayName("Exactly $50 purchase earns 0 points")
        void exactlyFiftyEarnsZero() {
            assertThat(rewardsForAmount(new BigDecimal("50.00")).getTotalPoints()).isZero();
        }

        @Test
        @DisplayName("Exactly $100 purchase earns 50 points")
        void exactlyHundredEarnsFifty() {
            assertThat(rewardsForAmount(new BigDecimal("100.00")).getTotalPoints()).isEqualTo(50);
        }

        @Test
        @DisplayName("$0.99 purchase earns 0 points")
        void lessThanOneDollarEarnsZero() {
            assertThat(rewardsForAmount(new BigDecimal("0.99")).getTotalPoints()).isZero();
        }

        @Test
        @DisplayName("$1000 purchase earns 1850 points")
        void thousandDollarPurchase() {
            assertThat(rewardsForAmount(new BigDecimal("1000.00")).getTotalPoints()).isEqualTo(1850);
        }
    }
}
