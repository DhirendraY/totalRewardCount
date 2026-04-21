package com.retailer.rewards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Retailer Rewards Program Spring Boot application.
 *
 * <p>This application exposes RESTful endpoints to calculate reward points
 * earned by customers based on their transaction history over a rolling
 * three-month period.</p>
 *
 * <p>Reward rules:
 * <ul>
 *   <li>2 points for every dollar spent <strong>over $100</strong> in a single transaction.</li>
 *   <li>1 point for every dollar spent <strong>between $50 and $100</strong> in a single transaction.</li>
 * </ul>
 * Example: a $120 purchase = 2×$20 + 1×$50 = 90 points.
 * </p>
 */
@SpringBootApplication
public class RewardsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RewardsApplication.class, args);
    }
}
