package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP server, real startup runner, and persisted H2 records (no mocked repositories). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:startup-http;DB_CLOSE_DELAY=-1", "spring.jpa.show-sql=false"})
@Import(JobCsvLoaderIntegrationTest.TimeConfig.class)
class RegistrationStartupHttpTest {
    @LocalServerPort int port;
    @Autowired UserAccountRepository accounts;
    @Autowired RecruiterRepository recruiters;
    @Autowired CompanyRepository companies;
    @Autowired JobPostingRepository postings;
    @Autowired ApplicationRepository applications;
    @Autowired AdministratorRepository administrators;
    @Autowired PasswordEncoder encoder;
    @Autowired CommandLineRunner seedDevelopmentData;

    @Test
    void startupRegistrationSearchAndRepeatedSeedingWorkEndToEnd() throws Exception {
        assertEquals(4, accounts.count());
        assertEquals(1, administrators.count());
        assertEquals(1, recruiters.count());
        assertEquals(1, companies.count());
        assertEquals(5, postings.count());
        var client = HttpClient.newHttpClient();
        var mapper = JsonMapper.builder().build();
        var search = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/jobs/search/results")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, search.statusCode());
        var results = mapper.readTree(search.body());
        assertEquals(3, results.size());
        assertEquals("Software Engineering Intern", results.get(0).get("title").asString());
        assertEquals("Backend Developer", results.get(1).get("title").asString());
        assertEquals("Frontend Developer", results.get(2).get("title").asString());
        assertFalse(search.body().contains("password"));

        for (String role : List.of("APPLICANT", "RECRUITER")) {
            String email = role.toLowerCase(java.util.Locale.ROOT) + ".http@example.com";
            String body = "{\"email\":\"" + email + "\",\"password\":\"Real HTTP secret\",\"role\":\"" + role
                    + "\",\"companyName\":\" baylor TECH \",\"fullName\":\"HTTP User\"}";
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/accounts/register"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(201, response.statusCode(), response.body());
            assertFalse(response.body().contains("password"));
            assertFalse(response.body().contains("Real HTTP secret"));
            var account = accounts.findByEmailIgnoreCase(email).orElseThrow();
            assertEquals(account.getId().longValue(), mapper.readTree(response.body()).get("accountId").asLong());
            assertTrue(encoder.matches("Real HTTP secret", account.getPasswordHash()));
            if (role.equals("RECRUITER")) assertEquals("Baylor Tech", assertInstanceOf(Recruiter.class, account).getCompany().getName());
            else assertInstanceOf(Applicant.class, account);
        }

        var originalIds = postings.findAll().stream().map(JobPosting::getId).toList();
        var applicant = accounts.findByEmailIgnoreCase("applicant@example.com").orElseThrow();
        String originalHash = applicant.getPasswordHash();
        applicant.updateRoleAndStatus(null, AccountStatus.DEACTIVATED, null, LocalDateTime.of(2026, 10, 12, 9, 0));
        accounts.saveAndFlush(applicant);
        seedDevelopmentData.run();
        assertEquals(originalIds, postings.findAll().stream().map(JobPosting::getId).toList());
        assertEquals(6, accounts.count());
        assertEquals(2, recruiters.count());
        assertEquals(1, companies.count());
        assertEquals(1, administrators.count());
        var preserved = accounts.findById(applicant.getId()).orElseThrow();
        assertEquals(originalHash, preserved.getPasswordHash());
        assertEquals(Role.APPLICANT, preserved.getRole());
        assertEquals(AccountStatus.DEACTIVATED, preserved.getStatus());

        applications.deleteAll();
        postings.deleteAll();
        seedDevelopmentData.run();
        assertEquals(5, postings.count(), "existing accounts must not prevent initializing an empty job table");
        assertEquals(6, accounts.count());
        assertEquals(2, recruiters.count());
    }
}
