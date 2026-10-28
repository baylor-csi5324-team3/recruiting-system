package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** User Login main flow and extensions 5a and 5b, through HTTP, the session and H2. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:login-integration;DB_CLOSE_DELAY=-1", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository accounts;
    @Autowired ApplicantRepository applicants;
    @Autowired RecruiterRepository recruiters;
    @Autowired CompanyRepository companies;
    @Autowired PasswordEncoder encoder;
    private static final String SECRET = "Login secret 2026";
    private Applicant applicant;
    private Recruiter recruiter;

    @BeforeEach
    void setUp() {
        applicants.deleteAll();
        recruiters.deleteAll();
        accounts.deleteAll();
        companies.deleteAll();
        applicant = applicants.save(new Applicant("ann@example.com", encoder.encode(SECRET), "Ann Lee", AccountStatus.ACTIVE));
        recruiter = recruiters.save(new Recruiter("hr@example.com", encoder.encode(SECRET), "Rhea",
                companies.save(new Company("Baylor Tech")), AccountStatus.ACTIVE));
        applicants.save(new Applicant("dormant@example.com", encoder.encode(SECRET), "Dormant", AccountStatus.DEACTIVATED));
    }

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(post("/api/accounts/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
    }

    @Test
    void applicantSignsInAndTheSessionReportsTheApplicant() throws Exception {
        MvcResult result = login(" ANN@example.com ", SECRET);
        assertEquals(200, result.getResponse().getStatus());
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("\"role\":\"APPLICANT\""));
        assertTrue(body.contains("\"id\":" + applicant.getId()));
        assertFalse(body.contains(SECRET));
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/accounts/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ann@example.com"))
                .andExpect(jsonPath("$.name").value("Ann Lee"));
    }

    @Test
    void recruiterSignInReportsTheRecruiterRecord() throws Exception {
        MvcResult result = login("hr@example.com", SECRET);
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("\"id\":" + recruiter.getId()));
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameMessage() throws Exception {
        MvcResult wrongPassword = login("ann@example.com", "not the password");
        MvcResult unknownEmail = login("nobody@example.com", SECRET);
        assertEquals(401, wrongPassword.getResponse().getStatus());
        assertEquals(401, unknownEmail.getResponse().getStatus());
        assertEquals(wrongPassword.getResponse().getContentAsString(), unknownEmail.getResponse().getContentAsString());
        assertNull(wrongPassword.getRequest().getSession(false));
    }

    @Test
    void deactivatedAccountIsRefusedEvenWithTheRightPassword() throws Exception {
        MvcResult result = login("dormant@example.com", SECRET);
        assertEquals(403, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("inactive"));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = (MockHttpSession) login("ann@example.com", SECRET).getRequest().getSession(false);
        mvc.perform(post("/api/accounts/logout").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/accounts/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void savingTheProfileUpdatesTheSignedInName() throws Exception {
        MockHttpSession session = (MockHttpSession) login("ann@example.com", SECRET).getRequest().getSession(false);
        mvc.perform(put("/api/profile").session(session).header("X-Applicant-Id", applicant.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ann Lee-Park\",\"phone\":\"\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/accounts/me").session(session)).andExpect(jsonPath("$.name").value("Ann Lee-Park"));
    }

    @Test
    void missingEmailOrPasswordIsRejected() throws Exception {
        for (String body : new String[]{"{\"email\":\" \",\"password\":\"x\"}", "{\"email\":\"ann@example.com\"}", "{\"password\":"}) {
            mvc.perform(post("/api/accounts/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Provide your email address and password."));
        }
    }
}
