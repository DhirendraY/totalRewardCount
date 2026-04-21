package com.retailer.rewards.exception;

/**
 * Exception thrown when a provided date range is invalid.
 *
 * <p>For example, this is raised when the {@code startDate} is after
 * the {@code endDate}.</p>
 */
public class InvalidDateRangeException extends RuntimeException {

    /**
     * Constructs a new {@code InvalidDateRangeException} with a descriptive message.
     *
     * @param message a human-readable explanation of why the date range is invalid
     */
    public InvalidDateRangeException(String message) {
        super(message);
    }
}
