package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** UC 4.2.1.3 User Login. Service lifeline of SD 6.3: message 4 authenticate and its sub-steps 4.1 to 4.3. */
@Service
@Transactional(readOnly = true)
public class AuthenticationService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Steps 5 and 5a/5b of the use case. An unknown email and a wrong password get the same answer, so the
     * response never reveals which emails are registered. The account found is already the applicant,
     * recruiter or administrator object, so no second lookup by role is needed.
     */
    public AuthenticatedUser authenticate(String email, String password) {
        UserAccount account = accounts.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))   // 4.1
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {                    // 4.2
            throw new InvalidCredentialsException();
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {                                      // 4.3
            throw new InactiveAccountException();
        }
        return new AuthenticatedUser(account.getId(), account.getEmail(), account.getRole(), account.getName());
    }
}
