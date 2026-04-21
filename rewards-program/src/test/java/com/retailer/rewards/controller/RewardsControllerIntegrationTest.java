package com.retailer.rewards.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link RewardsController}.
 *
 * <p>Boots the full Spring application context and exercises every endpoint
 * through {@link MockMvc}, verifying HTTP status codes, JSON structure,
 * and error-handling behaviour.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class RewardsControllerIntegrationTest {

    private static final String BASE_URL = "/api/v1/rewards";

    @Autowired
    private MockMvc mockMvc;

    // =========================================================================
    // GET /api/v1/rewards
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/rewards – all customers")
    class GetAllRewardsTests {

        @Test
        @DisplayName("Returns 200 and a non-empty array")
        void shouldReturn200WithAllCustomers() throws Exception {
            mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Response contains expected customer IDs")
        void shouldContainExpectedCustomerIds() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].customerId",
                            hasItems("C001", "C002", "C003", "C004", "C005")));
        }

        @Test
        @DisplayName("Each customer has monthlyRewards and totalPoints fields")
        void shouldHaveRequiredFields() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].monthlyRewards").isArray())
                    .andExpect(jsonPath("$[0].totalPoints").isNumber());
        }
    }

    // =========================================================================
    // GET /api/v1/rewards/{customerId}
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/rewards/{customerId} – single customer")
    class GetCustomerRewardsTests {

        @Test
        @DisplayName("Returns 200 for known customer C001")
        void shouldReturn200ForKnownCustomer() throws Exception {
            mockMvc.perform(get(BASE_URL + "/C001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId", is("C001")))
                    .andExpect(jsonPath("$.customerName", is("Alice Smith")))
                    .andExpect(jsonPath("$.totalPoints", is(435)));
        }

        @Test
        @DisplayName("Returns 200 for C005 (Eva – large purchase)")
        void shouldReturn200ForEva() throws Exception {
            mockMvc.perform(get(BASE_URL + "/C005"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPoints", is(1905)));
        }

        @Test
        @DisplayName("Returns 404 for unknown customer")
        void shouldReturn404ForUnknownCustomer() throws Exception {
            mockMvc.perform(get(BASE_URL + "/UNKNOWN"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", containsString("UNKNOWN")));
        }

        @Test
        @DisplayName("Monthly rewards are present and non-empty for C001")
        void shouldHaveMonthlyRewardsForC001() throws Exception {
            mockMvc.perform(get(BASE_URL + "/C001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.monthlyRewards", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$.monthlyRewards[0].monthLabel").isString())
                    .andExpect(jsonPath("$.monthlyRewards[0].points").isNumber());
        }
    }

    // =========================================================================
    // GET /api/v1/rewards/range
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/rewards/range – date range")
    class GetRewardsByDateRangeTests {

        @Test
        @DisplayName("Returns 200 for a wide historical range")
        void shouldReturn200ForWideRange() throws Exception {
            String start = LocalDate.now().minusYears(1).toString();
            String end   = LocalDate.now().plusDays(1).toString();

            mockMvc.perform(get(BASE_URL + "/range")
                            .param("startDate", start)
                            .param("endDate", end))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Returns empty array for future-only range")
        void shouldReturnEmptyForFutureRange() throws Exception {
            String start = LocalDate.now().plusYears(1).toString();
            String end   = LocalDate.now().plusYears(2).toString();

            mockMvc.perform(get(BASE_URL + "/range")
                            .param("startDate", start)
                            .param("endDate", end))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Returns 400 when startDate is after endDate")
        void shouldReturn400ForInvertedRange() throws Exception {
            String start = LocalDate.now().toString();
            String end   = LocalDate.now().minusDays(1).toString();

            mockMvc.perform(get(BASE_URL + "/range")
                            .param("startDate", start)
                            .param("endDate", end))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }

        @Test
        @DisplayName("Returns 400 when startDate parameter is missing")
        void shouldReturn400WhenStartDateMissing() throws Exception {
            mockMvc.perform(get(BASE_URL + "/range")
                            .param("endDate", LocalDate.now().toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 when endDate parameter is missing")
        void shouldReturn400WhenEndDateMissing() throws Exception {
            mockMvc.perform(get(BASE_URL + "/range")
                            .param("startDate", LocalDate.now().toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 for invalid date format")
        void shouldReturn400ForInvalidDateFormat() throws Exception {
            mockMvc.perform(get(BASE_URL + "/range")
                            .param("startDate", "not-a-date")
                            .param("endDate", LocalDate.now().toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /api/v1/rewards/{customerId}/range
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/rewards/{customerId}/range – customer + date range")
    class GetRewardsByCustomerAndDateRangeTests {

        @Test
        @DisplayName("Returns 200 for C002 within a wide range")
        void shouldReturn200ForKnownCustomerInRange() throws Exception {
            String start = LocalDate.now().minusYears(1).toString();
            String end   = LocalDate.now().plusDays(1).toString();

            mockMvc.perform(get(BASE_URL + "/C002/range")
                            .param("startDate", start)
                            .param("endDate", end))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId", is("C002")))
                    .andExpect(jsonPath("$.totalPoints").isNumber());
        }

        @Test
        @DisplayName("Returns 404 for unknown customer within a valid range")
        void shouldReturn404ForUnknownCustomerInRange() throws Exception {
            String start = LocalDate.now().minusYears(1).toString();
            String end   = LocalDate.now().plusDays(1).toString();

            mockMvc.perform(get(BASE_URL + "/NOBODY/range")
                            .param("startDate", start)
                            .param("endDate", end))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Returns 404 when customer exists but has no transactions in range")
        void shouldReturn404WhenCustomerHasNoTransactionsInRange() throws Exception {
            mockMvc.perform(get(BASE_URL + "/C001/range")
                            .param("startDate", "2000-01-01")
                            .param("endDate", "2000-03-31"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Returns 400 for inverted date range")
        void shouldReturn400ForInvertedRange() throws Exception {
            mockMvc.perform(get(BASE_URL + "/C001/range")
                            .param("startDate", LocalDate.now().toString())
                            .param("endDate", LocalDate.now().minusDays(1).toString()))
                    .andExpect(status().isBadRequest());
        }
    }
}
