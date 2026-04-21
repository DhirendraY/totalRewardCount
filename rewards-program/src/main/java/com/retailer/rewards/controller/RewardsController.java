package com.retailer.rewards.controller;

import com.retailer.rewards.model.CustomerRewards;
import com.retailer.rewards.service.RewardsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller that exposes reward-points endpoints for the Retailer Rewards Program.

 * <p>Available endpoints:
 * <ul>
 *   <li>{@code GET /api/v1/rewards}                   – rewards for all customers</li>
 *   <li>{@code GET /api/v1/rewards/{customerId}}       – rewards for one customer</li>
 *   <li>{@code GET /api/v1/rewards/range}              – rewards for all customers in a date range</li>
 *   <li>{@code GET /api/v1/rewards/{customerId}/range} – rewards for one customer in a date range</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/rewards")
public class RewardsController {

    private final RewardsService rewardsService;


    public RewardsController(RewardsService rewardsService) {
        this.rewardsService = rewardsService;
    }


    @GetMapping
    public ResponseEntity<List<CustomerRewards>> getAllCustomerRewards() {
        return ResponseEntity.ok(rewardsService.getAllCustomerRewards());
    }

    /**
     * Returns the reward-point summary for a single customer identified by their ID.
     *
     * <p>Example: {@code GET /api/v1/rewards/C001}</p>
     *
     * @param customerId the unique identifier of the customer (path variable)
     * @return 200 OK with the {@link CustomerRewards} for the given customer,
     *         or 404 NOT FOUND if the customer does not exist
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerRewards> getRewardsByCustomer(
            @PathVariable String customerId) {
        return ResponseEntity.ok(rewardsService.getRewardsByCustomerId(customerId));
    }

    /**
     * Returns the reward-point summary for all customers within a specified date range.
     *
     * <p>Example: {@code GET /api/v1/rewards/range?startDate=2024-01-01&endDate=2024-03-31}</p>
     *
     * @param startDate inclusive start date in {@code yyyy-MM-dd} format
     * @param endDate   inclusive end date in {@code yyyy-MM-dd} format
     * @return 200 OK with a filtered list of {@link CustomerRewards},
     *         or 400 BAD REQUEST if the date range is invalid
     */
    @GetMapping("/range")
    public ResponseEntity<List<CustomerRewards>> getRewardsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(rewardsService.getRewardsByDateRange(startDate, endDate));
    }

    /**
     * Returns the reward-point summary for a single customer within a specified date range.
     *
     * <p>Example: {@code GET /api/v1/rewards/C001/range?startDate=2024-01-01&endDate=2024-03-31}</p>
     *
     * @param customerId the unique identifier of the customer (path variable)
     * @param startDate  inclusive start date in {@code yyyy-MM-dd} format
     * @param endDate    inclusive end date in {@code yyyy-MM-dd} format
     * @return 200 OK with the filtered {@link CustomerRewards},
     *         or 400 BAD REQUEST / 404 NOT FOUND as appropriate
     */
    @GetMapping("/{customerId}/range")
    public ResponseEntity<CustomerRewards> getRewardsByCustomerAndDateRange(
            @PathVariable String customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                rewardsService.getRewardsByCustomerAndDateRange(customerId, startDate, endDate));
    }
}
