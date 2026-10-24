package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Register Account main flow and extensions, using actual service transactions and H2. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:registration-integration;DB_CLOSE_DELAY=-1", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTest {
    @Autowired RegistrationService service;
    @Autowired UserAccountRepository accounts;
    @Autowired CompanyRepository companies;
    @MockitoSpyBean RecruiterRepository recruiters;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private static final String SECRET = "Registration secret 2026";

    @BeforeEach
    void clear() {
        recruiters.deleteAll();
        accounts.deleteAll();
        companies.deleteAll();
    }

    private RegisterAccountRequest request(String email, Role role, String company) {
        return new RegisterAccountRequest(email, SECRET, role, company, "Rhea Recruiter");
    }

    @Test
    void applicantPersistsWithBCryptAndNoCompanyOrRecruiter() {
        UserAccount account = service.register(request(" Person@Example.COM ", Role.APPLICANT, "Ignored Company"));
        UserAccount saved = accounts.findById(account.getId()).orElseThrow();
        assertEquals("person@example.com", saved.getEmail());
        assertEquals(Role.APPLICANT, saved.getRole());
        assertEquals(AccountStatus.ACTIVE, saved.getStatus());
        assertNotEquals(SECRET, saved.getPasswordHash());
        assertTrue(encoder.matches(SECRET, saved.getPasswordHash()));
        assertFalse(encoder.matches("wrong", saved.getPasswordHash()));
        assertEquals(0, companies.count());
        assertEquals(0, recruiters.count());
    }

    @Test
    void recruiterCreatesCompanyAndBothRelationships() {
        UserAccount account = service.register(request("hr@example.com", Role.RECRUITER, " Baylor Tech "));
        Recruiter recruiter = recruiters.findByAccountId(account.getId()).orElseThrow();
        assertEquals(account.getId(), recruiter.getAccount().getId());
        assertEquals("Rhea Recruiter", recruiter.getName());
        assertEquals("Baylor Tech", recruiter.getCompany().getName());
        assertEquals(1, accounts.count());
        assertEquals(1, companies.count());
    }

    @Test
    void recruitersReuseCompanyIgnoringCaseAndSurroundingSpace() {
        Company company = companies.saveAndFlush(new Company("Baylor Tech"));
        UserAccount first = service.register(request("first@example.com", Role.RECRUITER, " baylor TECH "));
        assertEquals(company.getId(), recruiters.findByAccountId(first.getId()).orElseThrow().getCompany().getId());
        assertEquals(1, companies.count());
        assertEquals("Baylor Tech", companies.findById(company.getId()).orElseThrow().getName());
    }

    @Test
    void duplicateEmailIncludingCaseRejectedBeforeCompanyCreation() {
        service.register(request("person@example.com", Role.APPLICANT, null));
        for (String email : new String[]{"person@example.com", " PERSON@EXAMPLE.COM "}) {
            assertThrows(ValidationException.class, () -> service.register(request(email, Role.RECRUITER, "New Company")));
        }
        assertEquals(1, accounts.count());
        assertEquals(0, companies.count());
        assertEquals(0, recruiters.count());
    }

    @Test
    void httpApplicantRegistrationReturns201AndSafePersistedAccount() throws Exception {
        mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"http@example.com","password":"HTTP secret","role":"APPLICANT"}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.accountId").isNumber())
                .andExpect(jsonPath("$.email").value("http@example.com"))
                .andExpect(jsonPath("$.role").value("APPLICANT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertTrue(encoder.matches("HTTP secret", accounts.findByEmailIgnoreCase("http@example.com").orElseThrow().getPasswordHash()));
    }

    @Test
    void httpRecruiterRegistrationPersistsRelationships() throws Exception {
        mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"http.hr@example.com","password":"HTTP secret","role":"RECRUITER",
                 "companyName":"Baylor Tech","recruiterName":"Rhea"}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("RECRUITER"));
        var account = accounts.findByEmailIgnoreCase("http.hr@example.com").orElseThrow();
        assertEquals("Baylor Tech", recruiters.findByAccountId(account.getId()).orElseThrow().getCompany().getName());
    }

    @Test
    void httpValidationAndDuplicateUseMessageErrors() throws Exception {
        service.register(request("duplicate@example.com", Role.APPLICANT, null));
        for (String body : new String[]{"{}",
                "{\"email\":\"studentgmail.com\",\"password\":\"secret phrase\",\"role\":\"APPLICANT\"}",
                "{\"email\":\"admin.new@example.com\",\"password\":\"secret phrase\",\"role\":\"ADMINISTRATOR\"}",
                "{\"email\":\"hr@example.com\",\"password\":\"secret phrase\",\"role\":\"RECRUITER\"}"}) {
            mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        }
        mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":" DUPLICATE@EXAMPLE.COM ","password":"secret phrase","role":"APPLICANT"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An account with that email already exists."));
        assertEquals(1, accounts.count());
        assertEquals(0, companies.count());
        assertEquals(0, recruiters.count());
    }

    @Test
    void malformedRequestsDoNotEchoCredentials() throws Exception {
        for (String body : new String[]{"{\"password\":\"never-echo-this\",", "{\"role\":\"never-echo-this\"}"}) {
            String response = mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
            assertFalse(response.contains("never-echo-this"));
        }
    }

    @Test
    void databaseConflictIsSafeHttp409AndRollsBack() throws Exception {
        doThrow(new DataIntegrityViolationException("private database values must not escape"))
                .when(recruiters).saveAndFlush(any(Recruiter.class));
        String response = mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"hr@example.com","password":"never-echo-this","role":"RECRUITER",
                 "companyName":"New Company","recruiterName":"Rhea"}
                """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").isString())
                .andReturn().getResponse().getContentAsString();
        assertFalse(response.contains("private database"));
        assertFalse(response.contains("never-echo-this"));
        assertEquals(0, accounts.count());
        assertEquals(0, companies.count());
        assertEquals(0, recruiters.count());
    }

    @Test
    void entitySerializationDoesNotRevealHashOrRecruiterAccount() {
        var account = service.register(request("hr@example.com", Role.RECRUITER, "Baylor Tech"));
        var mapper = JsonMapper.builder().build();
        String accountJson = mapper.writeValueAsString(account);
        String recruiterJson = mapper.writeValueAsString(recruiters.findByAccountId(account.getId()).orElseThrow());
        assertFalse(accountJson.contains("passwordHash"));
        assertFalse(accountJson.contains(account.getPasswordHash()));
        assertFalse(recruiterJson.contains("\"account\""));
        assertFalse(recruiterJson.contains(account.getPasswordHash()));
    }

    @Test
    void databaseEnforcesCaseInsensitiveEmailAndCompanyUniqueness() {
        var account = service.register(request("first@example.com", Role.RECRUITER, "Baylor Tech"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into user_account(email,password_hash,role,status) values (?,?,?,?)",
                "FIRST@EXAMPLE.COM", account.getPasswordHash(), "APPLICANT", "ACTIVE"));
        assertThrows(DataIntegrityViolationException.class, () -> companies.saveAndFlush(new Company(" BAYLOR TECH ")));
        assertEquals(1, accounts.count());
        assertEquals(1, companies.count());
    }

    @Test
    void unicodePasswordAndSpacesSurviveBCryptEncodingExactly() {
        String password = "  𠮷abcde  ";
        var account = service.register(new RegisterAccountRequest("unicode@example.com", password,
                Role.APPLICANT, null, null));
        var saved = accounts.findById(account.getId()).orElseThrow();
        assertTrue(encoder.matches(password, saved.getPasswordHash()));
        assertFalse(encoder.matches(password.trim(), saved.getPasswordHash()));
    }
}
