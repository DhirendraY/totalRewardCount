package com.retailer.rewards.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated reward summary for a single customer.
 *
 * <p>Contains the monthly breakdown of points as well as the overall total
 * across all recorded transactions.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRewards {

    /**
     * Unique identifier for the customer.
     */
    private String customerId;

    /**
     * Display name of the customer.
     */
    private String customerName;

    /**
     * Reward points broken down by calendar month.
     */
    private List<MonthlyRewards> monthlyRewards;

    /**
     * Grand total reward points across all months.
     */
    private long totalPoints;
}
