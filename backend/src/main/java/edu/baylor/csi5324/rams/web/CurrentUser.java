package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.service.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

/** The signed-in user of the current request, kept in the server session by User Login. */
public final class CurrentUser {

    private static final String SESSION_KEY = "rams.currentUser";

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> of(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? Optional.empty()
                : Optional.ofNullable((AuthenticatedUser) session.getAttribute(SESSION_KEY));
    }

    /** SD 6.3 message 5: starts a new session, so a session id that existed before sign in is never reused. */
    static void signIn(HttpServletRequest request, AuthenticatedUser user) {
        signOut(request);
        request.getSession(true).setAttribute(SESSION_KEY, user);
    }

    /** Keeps the signed-in applicant's name current after Update Applicant Profile. */
    static void renameApplicant(HttpServletRequest request, Long applicantId, String name) {
        of(request).filter(user -> user.role() == Role.APPLICANT && user.id().equals(applicantId))
                .ifPresent(user -> request.getSession(false).setAttribute(SESSION_KEY,
                        new AuthenticatedUser(user.id(), user.email(), user.role(), name)));
    }

    static void signOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
