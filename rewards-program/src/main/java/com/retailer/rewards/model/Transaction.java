package com.retailer.rewards.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single purchase transaction made by a customer.
 *
 * <p>Each transaction records the customer identifier, the amount spent,
 * and the date on which the purchase occurred.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    /**
     * Unique identifier for this transaction.
     */
    private String transactionId;

    /**
     * Identifier of the customer who made the purchase.
     * Must not be blank.
     */
    @NotBlank(message = "Customer ID must not be blank")
    private String customerId;

    /**
     * Name of the customer (display purposes).
     */
    private String customerName;

    /**
     * Amount spent in this transaction (in USD).
     * Must be greater than 0.
     */
    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Date on which the transaction was recorded.
     */
    @NotNull(message = "Transaction date must not be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
}
