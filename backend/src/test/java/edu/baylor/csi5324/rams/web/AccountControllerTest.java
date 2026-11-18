package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.repository.AdministratorRepository;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import edu.baylor.csi5324.rams.service.RegistrationService;
import edu.baylor.csi5324.rams.service.AccountService;
import edu.baylor.csi5324.rams.service.NotFoundException;
import edu.baylor.csi5324.rams.service.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-15 Manage Accounts and Roles, web layer. */
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AccountService accountService;
    @MockitoBean AdministratorRepository administrators;
    @MockitoBean UserAccountRepository accounts;
    @MockitoBean RegistrationService registration;

    private final Administrator admin = new Administrator("admin@example.com", "hash", "Ada", AccountStatus.ACTIVE);

    @BeforeEach
    void administratorExists() {
        when(administrators.findById(1L)).thenReturn(Optional.of(admin));
        when(administrators.findById(99L)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("GET /api/accounts lists every user with the role")
    void listsUsers() throws Exception {
        when(accounts.findAll()).thenReturn(java.util.List.of(
                new Applicant("applicant@example.com", "hash", "Ann", AccountStatus.ACTIVE)));

        mockMvc.perform(get("/api/accounts").header(AccountController.ADMIN_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("applicant@example.com"))
                .andExpect(jsonPath("$[0].role").value("APPLICANT"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("GET /api/accounts by an unknown administrator is 404")
    void listUsersUnknownAdministrator() throws Exception {
        mockMvc.perform(get("/api/accounts").header(AccountController.ADMIN_HEADER, "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/accounts/recruiters creates a recruiter account (201)")
    void createsRecruiter() throws Exception {
        when(registration.register(any())).thenReturn(
                new Recruiter("rita@acme.com", "hash", "Rita", new Company("Acme"), AccountStatus.ACTIVE));

        mockMvc.perform(post("/api/accounts/recruiters").header(AccountController.ADMIN_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rita@acme.com\",\"password\":\"Secret-pass-1\",\"fullName\":\"Rita\",\"companyName\":\"Acme\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RECRUITER"))
                .andExpect(jsonPath("$.company").value("Acme"));
    }

    @Test
    @DisplayName("POST /api/accounts/recruiters with an email already in use is 400")
    void createRecruiterDuplicateEmail() throws Exception {
        when(registration.register(any())).thenThrow(new ValidationException("An account with that email already exists."));

        mockMvc.perform(post("/api/accounts/recruiters").header(AccountController.ADMIN_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rita@acme.com\",\"password\":\"Secret-pass-1\",\"fullName\":\"Rita\",\"companyName\":\"Acme\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/accounts/change asks which account to change (step 2)")
    void asksForAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/change").header(AccountController.ADMIN_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[0]").value("email"));
    }

    @Test
    @DisplayName("GET /api/accounts/lookup reports the current role and status (step 4)")
    void reportsRoleAndStatus() throws Exception {
        when(accountService.findAccount("applicant@example.com"))
                .thenReturn(new Applicant("applicant@example.com", "hash", "Ann", AccountStatus.ACTIVE));

        mockMvc.perform(get("/api/accounts/lookup").param("email", "applicant@example.com").header(AccountController.ADMIN_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("applicant@example.com"))
                .andExpect(jsonPath("$.role").value("APPLICANT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("an unknown email is reported as 404 (extension 3a)")
    void unknownEmail() throws Exception {
        when(accountService.findAccount("nobody@example.com")).thenThrow(new NotFoundException("No account matches nobody@example.com."));

        mockMvc.perform(get("/api/accounts/lookup").param("email", "nobody@example.com").header(AccountController.ADMIN_HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No account matches nobody@example.com."));
    }

    @Test
    @DisplayName("PUT /api/accounts/{id}/role-status applies the change and reports the new role and status (steps 5 to 8)")
    void appliesChange() throws Exception {
        UserAccount updated = new Administrator("applicant@example.com", "hash", "Ann", AccountStatus.ACTIVE);
        when(accountService.applyChange(eq(3L), eq(Role.ADMINISTRATOR), isNull(), isNull(), eq(admin))).thenReturn(updated);

        mockMvc.perform(put("/api/accounts/3/role-status").header(AccountController.ADMIN_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newRole\": \"ADMINISTRATOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMINISTRATOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("a rejected change is reported as 400 and the current role is kept (extensions 6a and 6b)")
    void rejectedChangeReported() throws Exception {
        when(accountService.applyChange(eq(3L), any(), any(), any(), eq(admin)))
                .thenThrow(new ValidationException("The change would leave the system without an active administrator."));

        mockMvc.perform(put("/api/accounts/3/role-status").header(AccountController.ADMIN_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\": \"DEACTIVATED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("active administrator")));
    }

    @Test
    @DisplayName("an unknown administrator is reported (precondition 1)")
    void unknownAdministrator() throws Exception {
        mockMvc.perform(get("/api/accounts/change").header(AccountController.ADMIN_HEADER, "99"))
                .andExpect(status().isNotFound());
    }
}
