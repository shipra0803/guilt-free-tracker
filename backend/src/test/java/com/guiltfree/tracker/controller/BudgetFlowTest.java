package com.guiltfree.tracker.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end budget rules through the real API: the summary maths, the 409 guard on categories,
// validation (400) and unknown ids (404). Each test gets a fresh, empty database.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:budgetflow")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BudgetFlowTest {

    static final String AUTH = "Basic " + Base64.getEncoder().encodeToString("sam:correct-horse".getBytes());

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void createAccount() throws Exception {
        mvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"sam\",\"password\":\"correct-horse\"}"));
    }

    // A logged-in API call; json may be null.
    ResultActions call(String method, String path, String json) throws Exception {
        var req = request(HttpMethod.valueOf(method), path).header("Authorization", AUTH);
        if (json != null) req.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(req);
    }

    @Test
    void summaryAddsUp() throws Exception {
        // $60,000, paid monthly, no state tax. Worked by hand (2026 single filer):
        // taxable 43,900 -> federal 1,240 + 31,500 * 12% = 5,020; FICA 3,720 + 870 = 4,590;
        // net 50,390 a year -> 4,199.17 this month.
        call("PUT", "/api/income", "{\"yearlySalary\":60000,\"stateTaxRatePercent\":0,\"payFrequency\":\"MONTHLY\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netThisMonth").value(4199.17));

        call("POST", "/api/fixed-expenses", "{\"name\":\"Rent\",\"monthlyAmount\":1500}");
        call("POST", "/api/fixed-expenses", "{\"name\":\"Phone\",\"monthlyAmount\":60}");
        call("PUT", "/api/fixed-expenses/2", "{\"name\":\"Phone\",\"monthlyAmount\":65}").andExpect(status().isOk());

        call("POST", "/api/flexible-categories", "{\"name\":\"Food\",\"monthlyBudget\":400}");
        call("POST", "/api/flexible-categories", "{\"name\":\"Fun\",\"monthlyBudget\":250}");

        // Food goes $50 over this month; last month's expense must not count.
        call("POST", "/api/expenses", "{\"amount\":450,\"description\":\"Groceries\",\"categoryId\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.categoryName").value("Food"));
        String lastMonth = LocalDate.now().withDayOfMonth(1).minusDays(1).toString();
        call("POST", "/api/expenses",
                "{\"amount\":20,\"description\":\"Old\",\"date\":\"" + lastMonth + "\",\"categoryId\":1}");

        call("GET", "/api/summary", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netThisMonth").value(4199.17))
                .andExpect(jsonPath("$.fixedTotal").value(1565.0))               // 1,500 + 65 (after the edit)
                .andExpect(jsonPath("$.remainingAfterFixed").value(2634.17))     // 4,199.17 - 1,565
                .andExpect(jsonPath("$.flexibleAllocated").value(650.0))         // 400 + 250
                .andExpect(jsonPath("$.flexibleSpentThisMonth").value(450.0))
                .andExpect(jsonPath("$.flexibleRemainingThisMonth").value(200.0))
                .andExpect(jsonPath("$.extraSavings").value(1984.17))            // 2,634.17 - 650
                .andExpect(jsonPath("$.categories[0].name").value("Food"))
                .andExpect(jsonPath("$.categories[0].spentThisMonth").value(450.0))
                .andExpect(jsonPath("$.categories[0].remaining").value(-50.0))   // overspend shows, not clamped
                .andExpect(jsonPath("$.categories[1].remaining").value(250.0));
    }

    @Test
    void categoryWithExpensesCannotBeDeleted() throws Exception {
        call("POST", "/api/flexible-categories", "{\"name\":\"Food\",\"monthlyBudget\":400}");
        call("POST", "/api/expenses", "{\"amount\":12.5,\"description\":\"Lunch\",\"categoryId\":1}");

        call("DELETE", "/api/flexible-categories/1", null).andExpect(status().isConflict());

        call("DELETE", "/api/expenses/1", null).andExpect(status().isNoContent());
        call("DELETE", "/api/flexible-categories/1", null).andExpect(status().isNoContent());
        call("GET", "/api/summary", null).andExpect(jsonPath("$.categories").isEmpty());
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        call("POST", "/api/flexible-categories", "{\"name\":\"Food\",\"monthlyBudget\":400}");

        call("POST", "/api/expenses", "{\"amount\":5,\"description\":\"No category\"}").andExpect(status().isBadRequest());
        call("POST", "/api/expenses", "{\"amount\":0,\"description\":\"Free\",\"categoryId\":1}").andExpect(status().isBadRequest());
        call("POST", "/api/expenses", "{\"amount\":5,\"description\":\"  \",\"categoryId\":1}").andExpect(status().isBadRequest());
        call("POST", "/api/fixed-expenses", "{\"name\":\"\",\"monthlyAmount\":10}").andExpect(status().isBadRequest());
        call("POST", "/api/fixed-expenses", "{\"name\":\"Rent\",\"monthlyAmount\":-1}").andExpect(status().isBadRequest());
        call("POST", "/api/flexible-categories", "{\"name\":\"Fun\"}").andExpect(status().isBadRequest());
        call("PUT", "/api/income", "{\"yearlySalary\":0,\"stateTaxRatePercent\":0,\"payFrequency\":\"MONTHLY\"}")
                .andExpect(status().isBadRequest());
        call("PUT", "/api/income", "{\"yearlySalary\":50000,\"stateTaxRatePercent\":-2,\"payFrequency\":\"MONTHLY\"}")
                .andExpect(status().isBadRequest());

        // Nothing invalid was saved.
        call("GET", "/api/expenses", null).andExpect(jsonPath("$").isEmpty());
        call("GET", "/api/fixed-expenses", null).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void unknownIdsAreNotFound() throws Exception {
        call("PUT", "/api/fixed-expenses/99", "{\"name\":\"Rent\",\"monthlyAmount\":10}").andExpect(status().isNotFound());
        call("PUT", "/api/flexible-categories/99", "{\"name\":\"Food\",\"monthlyBudget\":10}").andExpect(status().isNotFound());
        call("DELETE", "/api/flexible-categories/99", null).andExpect(status().isNotFound());
        call("POST", "/api/expenses", "{\"amount\":5,\"description\":\"Lost\",\"categoryId\":99}").andExpect(status().isNotFound());
    }
}
