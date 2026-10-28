package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Update Applicant Profile main flow and extension 4a, through HTTP and H2. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:profile-integration;DB_CLOSE_DELAY=-1", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicantProfileIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ApplicantRepository applicants;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    private Applicant applicant;

    @BeforeEach
    void setUp() {
        applicants.deleteAll();
        accounts.deleteAll();
        applicant = applicants.save(new Applicant("ann@example.com", encoder.encode("Profile secret"), "Ann Lee",
                "254-555-0100", AccountStatus.ACTIVE));
    }

    @Test
    void viewProfileReportsNameEmailAndPhone() throws Exception {
        mvc.perform(get("/api/profile").header("X-Applicant-Id", applicant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ann Lee"))
                .andExpect(jsonPath("$.email").value("ann@example.com"))
                .andExpect(jsonPath("$.phone").value("254-555-0100"));
    }

    @Test
    void submitProfileSavesTrimmedChanges() throws Exception {
        mvc.perform(put("/api/profile").header("X-Applicant-Id", applicant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Ann Marie Lee \",\"phone\":\" +1 (254) 555-0199 \","
                                + "\"location\":\" Waco, TX \",\"headline\":\"Data analyst\",\"bio\":\"  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ann Marie Lee"))
                .andExpect(jsonPath("$.location").value("Waco, TX"))
                .andExpect(jsonPath("$.bio").doesNotExist());
        Applicant saved = applicants.findById(applicant.getId()).orElseThrow();
        assertEquals("Ann Marie Lee", saved.getName());
        assertEquals("+1 (254) 555-0199", saved.getPhone());
        assertEquals("Data analyst", saved.getDetails().headline());
    }

    @Test
    void missingNameOrInvalidPhoneIsRejectedAndProfileUnchanged() throws Exception {
        for (String body : new String[]{"{\"name\":\"  \",\"phone\":\"254\"}", "{\"name\":\"Ann\",\"phone\":\"call me\"}"}) {
            mvc.perform(put("/api/profile").header("X-Applicant-Id", applicant.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isString());
        }
        Applicant unchanged = applicants.findById(applicant.getId()).orElseThrow();
        assertEquals("Ann Lee", unchanged.getName());
        assertEquals("254-555-0100", unchanged.getPhone());
    }

    @Test
    void unknownOrDeactivatedApplicantIsRejected() throws Exception {
        mvc.perform(get("/api/profile").header("X-Applicant-Id", 9999)).andExpect(status().isNotFound());
        Applicant dormant = applicants.save(new Applicant("dormant@example.com", encoder.encode("Profile secret"), "Dormant",
                AccountStatus.DEACTIVATED));
        mvc.perform(get("/api/profile").header("X-Applicant-Id", dormant.getId())).andExpect(status().isBadRequest());
    }
}
