package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.RegisterAccountRequest;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.CompanyRepository;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Register Account: validates and persists the complete registration in one transaction. */
@Service
public class RegistrationService {
    private final UserAccountRepository accounts;
    private final ApplicantRepository applicants;
    private final RecruiterRepository recruiters;
    private final CompanyRepository companies;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationValidator validator;

    public RegistrationService(UserAccountRepository accounts, ApplicantRepository applicants,
                               RecruiterRepository recruiters, CompanyRepository companies,
                               PasswordEncoder passwordEncoder, RegistrationValidator validator) {
        this.accounts = accounts;
        this.applicants = applicants;
        this.recruiters = recruiters;
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
        UserAccount account = accounts.saveAndFlush(
                new UserAccount(request.email(), hash, request.role(), AccountStatus.ACTIVE));
        if (request.role() == Role.RECRUITER) {
            recruiters.saveAndFlush(new Recruiter(request.recruiterName(), account, company));
        }
        if (request.role() == Role.APPLICANT) {
            // The applicant profile exists from the first day; Update Applicant Profile (4.2.1.4) edits it later.
            String name = request.applicantName() != null ? request.applicantName()
                    : request.email().substring(0, request.email().indexOf('@'));
            applicants.saveAndFlush(new Applicant(name, account));
        }
        return account;
    }
}
