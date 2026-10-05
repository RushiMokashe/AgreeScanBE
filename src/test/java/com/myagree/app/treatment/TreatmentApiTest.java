package com.myagree.app.treatment;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.FixedClockConfiguration;
import com.myagree.app.support.JsonBodies;
import com.myagree.app.support.TestUsers;

@AgriScanApiTest
class TreatmentApiTest {

    private static final String STEP_URL = "/api/treatment-plans/{planId}/steps/{stepId}/";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void activePlanShowsTheSevenDayProtocol() throws Exception {
        mvc.perform(get("/api/treatment-plans/active").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotCode").value("Plot A"))
                .andExpect(jsonPath("$.title").value("Treatment Plan: Plot A"))
                .andExpect(jsonPath("$.target").value("Tomato Early Blight"))
                .andExpect(jsonPath("$.pathogen").value("Alternaria solani"))
                .andExpect(jsonPath("$.startDate").value("2026-09-29"))
                .andExpect(jsonPath("$.durationDays").value(7))
                .andExpect(jsonPath("$.completedSteps").value(1))
                .andExpect(jsonPath("$.totalSteps").value(3))
                .andExpect(jsonPath("$.progressPercent").value(33))
                .andExpect(jsonPath("$.steps[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.steps[0].dayLabel").value("Today"))
                .andExpect(jsonPath("$.steps[0].completedAt").value("2026-09-29T02:00:00Z"))
                .andExpect(jsonPath("$.steps[0].detail").value("Mancozeb 75% WP • **40g per 15L Knapsack Pump**"))
                .andExpect(jsonPath("$.steps[1].status").value("UPCOMING"))
                .andExpect(jsonPath("$.steps[1].dayLabel").value("In 3 Days"))
                .andExpect(jsonPath("$.steps[1].scheduledDate").value("2026-10-02"))
                .andExpect(jsonPath("$.steps[1].badge").value("Critical Check"))
                .andExpect(jsonPath("$.steps[1].reminderTime").value("07:00 AM"))
                .andExpect(jsonPath("$.steps[1].reminderSet").value(false))
                .andExpect(jsonPath("$.steps[1].completedAt").value(nullValue()))
                .andExpect(jsonPath("$.steps[2].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.steps[2].dayLabel").value("Scheduled"))
                .andExpect(jsonPath("$.steps[2].scheduledDate").value("2026-10-05"));
    }

    @Test
    void completingTheNextStepAdvancesTheTimeline() throws Exception {
        MvcResult plan = activePlan();

        mvc.perform(post(STEP_URL + "complete", planId(plan), stepId(plan, 1)).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$.steps[1].completedAt").value(FixedClockConfiguration.NOW.toString()))
                .andExpect(jsonPath("$.steps[2].status").value("UPCOMING"))
                .andExpect(jsonPath("$.steps[2].dayLabel").value("In 6 Days"))
                .andExpect(jsonPath("$.completedSteps").value(2))
                .andExpect(jsonPath("$.progressPercent").value(67));
    }

    @Test
    void completingAFinishedStepKeepsItsOriginalCompletionTime() throws Exception {
        MvcResult plan = activePlan();

        mvc.perform(post(STEP_URL + "complete", planId(plan), stepId(plan, 0)).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].completedAt").value("2026-09-29T02:00:00Z"))
                .andExpect(jsonPath("$.completedSteps").value(1));
    }

    @Test
    void reminderTogglesOnAndOff() throws Exception {
        MvcResult plan = activePlan();
        long planId = planId(plan);
        long stepId = stepId(plan, 1);

        mvc.perform(post(STEP_URL + "reminder", planId, stepId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[1].reminderSet").value(true));
        mvc.perform(post(STEP_URL + "reminder", planId, stepId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[1].reminderSet").value(false));
    }

    @Test
    void planIsAvailableById() throws Exception {
        long planId = planId(activePlan());

        mvc.perform(get("/api/treatment-plans/{id}", planId).with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId))
                .andExpect(jsonPath("$.steps.length()").value(3));
    }

    @Test
    void unknownPlanOrStepIsNotFound() throws Exception {
        long planId = planId(activePlan());

        mvc.perform(get("/api/treatment-plans/{id}", 999).with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Treatment plan 999 not found"));
        mvc.perform(post(STEP_URL + "complete", planId, 999).with(users.demoFarmer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Step 999 not found in treatment plan " + planId));
    }

    private MvcResult activePlan() throws Exception {
        return mvc.perform(get("/api/treatment-plans/active").with(users.demoFarmer()))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static long planId(MvcResult plan) throws Exception {
        return JsonBodies.readId(plan, "$.id");
    }

    private static long stepId(MvcResult plan, int index) throws Exception {
        return JsonBodies.readId(plan, "$.steps[" + index + "].id");
    }
}
