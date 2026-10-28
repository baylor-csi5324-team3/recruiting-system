package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.service.AuthenticatedUser;
import edu.baylor.csi5324.rams.service.AuthenticationService;
import edu.baylor.csi5324.rams.service.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC 4.2.1.3 User Login. Controller lifeline of SD 6.3:
 * 2 submitLoginInfo, 3 validate, 4 authenticate. The signed-in user is kept in the server session.
 */
@RestController
@RequestMapping("/api/accounts")
public class LoginController {
    private final AuthenticationService authenticationService;

    public LoginController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /** Steps 3 to 6: checks the login information and signs the user in. */
    @PostMapping("/login")
    public AuthenticatedUser submitLoginInfo(@RequestBody LoginRequest request, HttpServletRequest http) {
        validate(request);                                                                      // 3
        AuthenticatedUser user = authenticationService.authenticate(request.email(), request.password());   // 4
        CurrentUser.signIn(http, user);
        return user;
    }

    /** The signed-in user, so the front end can restore the session after a page reload. */
    @GetMapping("/me")
    public AuthenticatedUser currentUser(HttpServletRequest http) {
        return CurrentUser.of(http).orElseThrow(NotSignedInException::new);
    }

    /** Ends the session (FR 3.1.1: registered users can log out). */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest http) {
        CurrentUser.signOut(http);
    }

    /** Step 4: both values are required before the credentials are checked. */
    private void validate(LoginRequest request) {
        if (request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isEmpty()) {
            throw new ValidationException("Provide your email address and password.");
        }
    }

    /** No user is signed in for this session. */
    static class NotSignedInException extends RuntimeException {
        NotSignedInException() {
            super("Sign in to continue.");
        }
    }
}
