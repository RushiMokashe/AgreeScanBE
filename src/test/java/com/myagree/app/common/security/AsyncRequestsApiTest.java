package com.myagree.app.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.support.AgriScanApiTest;
import com.myagree.app.support.TestUsers;

/**
 * Answers written later, such as the notification stream's Server-Sent Events (docs/architecture/phase-2.md, D10),
 * are dispatched again through the security filters; the caller's sign-in must carry over to that dispatch.
 */
@AgriScanApiTest
@Import(AsyncRequestsApiTest.DelayedAnswerController.class)
class AsyncRequestsApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void anAnswerWrittenLaterKeepsTheCallersSignIn() throws Exception {
        MvcResult started = mvc.perform(get(DelayedAnswerController.PATH).with(users.owner()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().string("Rameshwar Patil"));
    }

    @Test
    void anAnonymousCallerIsStillTurnedAwayBeforeAnythingStarts() throws Exception {
        mvc.perform(get(DelayedAnswerController.PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(request().asyncNotStarted());
    }

    /** Answers on another thread, as an event stream does. */
    @RestController
    static class DelayedAnswerController {

        static final String PATH = "/api/notifications/delayed-answer";

        @GetMapping(PATH)
        Callable<String> delayedAnswer(CurrentUser user) {
            return user::name;
        }
    }
}
