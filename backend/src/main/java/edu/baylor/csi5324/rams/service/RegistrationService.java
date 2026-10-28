package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.RegisterAccountRequest;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.CompanyRepository;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Register Account: validates and persists the complete registration in one transaction. */
@Service
public class RegistrationService {
    private final UserAccountRepository accounts;
    private final CompanyRepository companies;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationValidator validator;

    public RegistrationService(UserAccountRepository accounts, CompanyRepository companies,
                               PasswordEncoder passwordEncoder, RegistrationValidator validator) {
        this.accounts = accounts;
        this.companies = companies;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional
    public UserAccount register(RegisterAccountRequest request) {
        validator.validate(request);
        if (accounts.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ValidationException("An account with that email already exists.");
        }
        String hash = passwordEncoder.encode(request.password());
        Company company = null;
        if (request.role() == Role.RECRUITER) {
            company = companies.findByNameIgnoreCase(request.companyName())
                    .orElseGet(() -> companies.saveAndFlush(new Company(request.companyName())));
        }
        // The role is the subclass: one object, saved once, holds the account and the role (SD 6.2 messages 4.6, 4.7).
        UserAccount account = request.role() == Role.RECRUITER
                ? new Recruiter(request.email(), hash, request.fullName(), company, AccountStatus.ACTIVE)
                : new Applicant(request.email(), hash, request.fullName(), AccountStatus.ACTIVE);
        return accounts.saveAndFlush(account);
    }
}
