package com.retailer.rewards.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the reward points earned by a customer in a specific calendar month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRewards {

    /**
     * The calendar year (e.g. 2024).
     */
    private int year;

    /**
     * The calendar month number (1 = January … 12 = December).
     */
    private int month;

    /**
     * Human-readable month label (e.g. "JANUARY 2024").
     */
    private String monthLabel;

    /**
     * Total reward points earned in this month.
     */
    private long points;
}
