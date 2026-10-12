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
        assertEquals(60, saved.getPasswordHash().length());
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
        UserAccount second = service.register(request("second@example.com", Role.RECRUITER, "BAYLOR TECH"));
        assertEquals(company.getId(), recruiters.findByAccountId(first.getId()).orElseThrow().getCompany().getId());
        assertEquals(company.getId(), recruiters.findByAccountId(second.getId()).orElseThrow().getCompany().getId());
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
    void failedRecruiterSaveRollsBackAlreadySavedAccountAndCompany() {
        doThrow(new DataIntegrityViolationException("simulated constraint failure"))
                .when(recruiters).saveAndFlush(any(Recruiter.class));
        assertThrows(DataIntegrityViolationException.class,
                () -> service.register(request("hr@example.com", Role.RECRUITER, "New Company")));
        assertEquals(0, accounts.count());
        assertEquals(0, companies.count());
        assertEquals(0, recruiters.count());
    }

    @Test
    void invalidRegistrationsNeverWrite() {
        for (RegisterAccountRequest input : new RegisterAccountRequest[]{null,
                request("bad", Role.APPLICANT, null), request("a@example.com", Role.ADMINISTRATOR, null),
                request("a@example.com", Role.RECRUITER, " "),
                new RegisterAccountRequest("a@example.com", "", Role.APPLICANT, null, null)}) {
            assertThrows(ValidationException.class, () -> service.register(input));
        }
        assertEquals(0, accounts.count());
        assertEquals(0, companies.count());
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
                "{\"email\":\"duplicate@example.com\",\"password\":\"secret phrase\",\"role\":\"APPLICANT\"}",
                "{\"email\":\"admin.new@example.com\",\"password\":\"secret phrase\",\"role\":\"ADMINISTRATOR\"}",
                "{\"email\":\"hr@example.com\",\"password\":\"secret phrase\",\"role\":\"RECRUITER\"}"}) {
            mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        }
        assertEquals(1, accounts.count());
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
    void databaseRequiresHashAndUniqueRecruiterAccountAndCompany() {
        assertThrows(DataIntegrityViolationException.class, () -> accounts.saveAndFlush(
                new UserAccount("incomplete@example.com", Role.APPLICANT, AccountStatus.ACTIVE)));
        Company company = companies.saveAndFlush(new Company("Baylor Tech"));
        assertThrows(DataIntegrityViolationException.class, () -> recruiters.saveAndFlush(new Recruiter("Incomplete", company)));
        var account = service.register(request("hr@example.com", Role.RECRUITER, "Baylor Tech"));
        assertThrows(DataIntegrityViolationException.class, () -> recruiters.saveAndFlush(new Recruiter("Duplicate", account, company)));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "update recruiter set company_id=null where account_id=?", account.getId()));
        assertEquals(1, recruiters.count());
    }

    @Test
    void httpPasswordMinimumRejectsSevenAndAcceptsEightWithoutPartialWrites() throws Exception {
        mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"boundary@example.com","password":"abcdefg","role":"APPLICANT"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must contain at least 8 Unicode characters."));
        assertEquals(0, accounts.count());
        mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"boundary@example.com","password":"abcdefgh","role":"APPLICANT"}
                """))
                .andExpect(status().isCreated());
        assertTrue(encoder.matches("abcdefgh", accounts.findByEmailIgnoreCase("boundary@example.com").orElseThrow().getPasswordHash()));
        assertEquals(0, recruiters.count());
        assertEquals(0, companies.count());
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

    @Test
    void recruiterUnicodeNameBoundaryPersistsWithoutSchemaChanges() {
        String name = "𠮷".repeat(100);
        var account = service.register(new RegisterAccountRequest("unicode.hr@example.com", SECRET,
                Role.RECRUITER, "L'Étoile & Sons — 科技", name));
        var recruiter = recruiters.findByAccountId(account.getId()).orElseThrow();
        assertEquals(name, recruiter.getName());
        assertEquals("L'Étoile & Sons — 科技", recruiter.getCompany().getName());
    }

    @Test
    void httpNameBoundariesRejectOversizedInputAndPersistValidCompany() throws Exception {
        var mapper = JsonMapper.builder().build();
        for (var names : new String[][]{{"x".repeat(151), "Rhea"}, {"New Company", "𠮷".repeat(101)}}) {
            String body = mapper.writeValueAsString(java.util.Map.of("email", "names@example.com", "password", SECRET,
                    "role", "RECRUITER", "companyName", names[0], "recruiterName", names[1]));
            mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
            assertEquals(0, accounts.count());
            assertEquals(0, companies.count());
            assertEquals(0, recruiters.count());
        }
        String companyName = "科".repeat(150);
        String body = mapper.writeValueAsString(java.util.Map.of("email", "names@example.com", "password", SECRET,
                "role", "RECRUITER", "companyName", companyName, "recruiterName", "Anne-Marie O'Connor"));
        mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        var account = accounts.findByEmailIgnoreCase("names@example.com").orElseThrow();
        var recruiter = recruiters.findByAccountId(account.getId()).orElseThrow();
        assertEquals(companyName, recruiter.getCompany().getName());
        assertEquals("Anne-Marie O'Connor", recruiter.getName());
    }

    @Test
    void httpUppercaseEmailDuplicateStillReportsDuplicate() throws Exception {
        service.register(request("alice@example.com", Role.APPLICANT, null));
        mvc.perform(post("/api/accounts/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":" ALICE@EXAMPLE.COM ","password":"secret phrase","role":"APPLICANT"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An account with that email already exists."));
        assertEquals(1, accounts.count());
    }
}
