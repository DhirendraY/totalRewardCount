package com.retailer.rewards.exception;

/**
 * Exception thrown when a requested customer cannot be found in the system.
 */
public class CustomerNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code CustomerNotFoundException} with a detail message
     * that includes the offending customer identifier.
     *
     * @param customerId the customer identifier that was not found
     */
    public CustomerNotFoundException(String customerId) {
        super("Customer not found with ID: " + customerId);
    }
}
