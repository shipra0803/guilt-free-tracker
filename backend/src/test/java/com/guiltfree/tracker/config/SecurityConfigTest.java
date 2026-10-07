package com.guiltfree.tracker.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:security")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    static String signupBody(String user, String password) {
        return "{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}";
    }

    // One sequential story: open sign-up -> create the account -> sign-up closes -> only that login works.
    @Test
    void firstRunSignupThenLoginOnly() throws Exception {
        mvc.perform(get("/api/signup")).andExpect(status().isOk()).andExpect(jsonPath("$.open").value(true));
        mvc.perform(get("/api/summary")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));

        mvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(signupBody("sam", "short")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(signupBody("sam", "correct-horse")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/signup")).andExpect(jsonPath("$.open").value(false));
        mvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(signupBody("eve", "another-password")))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/summary").header("Authorization", basic("sam", "wrong-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/summary").header("Authorization", basic("eve", "another-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/summary").header("Authorization", basic("sam", "correct-horse")))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightIsAllowedWithoutLogin() throws Exception {
        mvc.perform(options("/api/summary")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
