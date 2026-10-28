package edu.baylor.csi5324.rams.web;

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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The identity check that is switched on with rams.login.enforce=true. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:login-gate;DB_CLOSE_DELAY=-1",
        "spring.jpa.show-sql=false", "rams.login.enforce=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginGateTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository accounts;
    @Autowired ApplicantRepository applicants;
    @Autowired PasswordEncoder encoder;
    private static final String SECRET = "Gate secret 2026";
    private Applicant ann;
    private Applicant bob;

    @BeforeEach
    void setUp() {
        applicants.deleteAll();
        accounts.deleteAll();
        ann = applicants.save(new Applicant("ann@example.com", encoder.encode(SECRET), "Ann", AccountStatus.ACTIVE));
        bob = applicants.save(new Applicant("bob@example.com", encoder.encode(SECRET), "Bob", AccountStatus.ACTIVE));
    }

    private MockHttpSession signInAsAnn() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/accounts/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ann@example.com\",\"password\":\"" + SECRET + "\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void identityHeaderWithoutSignInIsRejectedButPublicEndPointsStayOpen() throws Exception {
        mvc.perform(get("/api/profile").header("X-Applicant-Id", ann.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/jobs/search/results")).andExpect(status().isOk());
    }

    @Test
    void signedInApplicantUsesOnlyTheirOwnId() throws Exception {
        MockHttpSession session = signInAsAnn();
        mvc.perform(get("/api/profile").header("X-Applicant-Id", ann.getId()).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ann"));
        mvc.perform(get("/api/profile").header("X-Applicant-Id", bob.getId()).session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void applicantCannotUseARecruiterHeader() throws Exception {
        mvc.perform(get("/api/applications/tracking").header("X-Recruiter-Id", ann.getId()).session(signInAsAnn()))
                .andExpect(status().isForbidden());
    }

    @Test
    void idsInParametersAndPathsMustBelongToTheSignedInApplicant() throws Exception {
        MockHttpSession session = signInAsAnn();
        mvc.perform(get("/api/demo/applications").param("applicantId", String.valueOf(ann.getId())).session(session))
                .andExpect(status().isOk());
        mvc.perform(get("/api/demo/applications").param("applicantId", String.valueOf(bob.getId())).session(session))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/applicants/" + ann.getId() + "/resumes").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/applicants/" + bob.getId() + "/resumes").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/demo/saved/" + bob.getId()).session(session)).andExpect(status().isForbidden());
    }

    @Test
    void userListsNeedASignedInUser() throws Exception {
        mvc.perform(get("/api/demo/actors")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/demo/actors").session(signInAsAnn())).andExpect(status().isOk());
        mvc.perform(get("/api/applicants").session(signInAsAnn())).andExpect(status().isForbidden());
    }

    @Test
    void submittedApplicationMustNameTheSignedInApplicant() throws Exception {
        String body = "{\"applicantId\":" + bob.getId() + ",\"jobPostingId\":1,\"resumeId\":1}";
        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(body).session(signInAsAnn()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This request does not belong to the signed-in user."));
    }

    @Test
    void deactivatingTheAccountEndsTheSession() throws Exception {
        MockHttpSession session = signInAsAnn();
        UserAccount account = accounts.findByEmailIgnoreCase("ann@example.com").orElseThrow();
        account.updateRoleAndStatus(null, AccountStatus.DEACTIVATED, null, LocalDateTime.now());
        accounts.saveAndFlush(account);
        mvc.perform(get("/api/profile").header("X-Applicant-Id", ann.getId()).session(session))
                .andExpect(status().isUnauthorized());
    }
}
