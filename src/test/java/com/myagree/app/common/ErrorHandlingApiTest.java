package com.myagree.app.common;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.TestUsers;

/** Every failure, including Spring MVC's own, comes back in the {@code ApiErrorBody} shape. */
@AgriScanApiTest
class ErrorHandlingApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void unknownRouteIsNotFound() throws Exception {
        mvc.perform(get("/api/does-not-exist").with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", not(emptyString())));
    }

    @Test
    void unsupportedMethodIsRejected() throws Exception {
        mvc.perform(delete("/api/dashboard").with(users.demoFarmer()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void nonNumericIdIsABadRequest() throws Exception {
        mvc.perform(get("/api/scans/{id}", "abc").with(users.demoFarmer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for 'id'"));
    }

    @Test
    void outOfRangeQueryParameterIsABadRequest() throws Exception {
        mvc.perform(get("/api/scans").param("limit", "0").with(users.demoFarmer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit must be greater than or equal to 1"));
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/cart/items")
                        .with(users.demoFarmer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }
}
