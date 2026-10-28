package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import edu.baylor.csi5324.rams.service.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks, before a request reaches a controller, that every user id it names belongs to the signed-in user, so the
 * existing end points can keep reading their ids. An id is named by an identity header (X-Applicant-Id,
 * X-Recruiter-Id, X-Administrator-Id), a query parameter (applicantId, recruiterId, administratorId) or an
 * applicant path (/api/applicants/{id}/..., /api/demo/saved/{id}). The lists of users need a signed-in user.
 * Requests that name no user (registration, login, job search) pass unchanged.
 */
public class IdentityCheckInterceptor implements HandlerInterceptor {

    private static final Map<String, Role> HEADERS = Map.of(
            ApplicationController.APPLICANT_HEADER, Role.APPLICANT,
            ApplicationController.RECRUITER_HEADER, Role.RECRUITER,
            AccountController.ADMIN_HEADER, Role.ADMINISTRATOR);

    private static final Map<String, Role> PARAMETERS = Map.of(
            "applicantId", Role.APPLICANT,
            "recruiterId", Role.RECRUITER,
            "administratorId", Role.ADMINISTRATOR);

    private static final List<Pattern> APPLICANT_PATHS = List.of(
            Pattern.compile("^/api/applicants/([^/]+)(/.*)?$"),
            Pattern.compile("^/api/demo/saved/([^/]+)$"));

    /** All applicants, used only by the two back-end demo pages: administrators only. */
    private static final String APPLICANT_LIST = "/api/applicants";
    /** All users with their roles, used by the React front end after sign in. */
    private static final String USER_LIST = "/api/demo/actors";

    private final UserAccountRepository accounts;

    public IdentityCheckInterceptor(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        List<Claim> claims = claims(request, path);
        boolean listOfUsers = path.equals(APPLICANT_LIST) || path.equals(USER_LIST);
        if (claims.isEmpty() && !listOfUsers) {
            return true;
        }
        Optional<AuthenticatedUser> user = CurrentUser.of(request);
        if (user.isEmpty()) {
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Sign in to continue.");
        }
        if (!stillActiveWithSameRole(user.get())) {
            // Manage Accounts and Roles, extension 7a: a deactivated or replaced account loses its session.
            CurrentUser.signOut(request);
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Your session has ended. Sign in again.");
        }
        if (path.equals(APPLICANT_LIST) && user.get().role() != Role.ADMINISTRATOR) {
            return reject(response, HttpServletResponse.SC_FORBIDDEN, "This request does not belong to the signed-in user.");
        }
        for (Claim claim : claims) {
            if (!claim.belongsTo(user.get())) {
                return reject(response, HttpServletResponse.SC_FORBIDDEN, "This request does not belong to the signed-in user.");
            }
        }
        return true;
    }

    /** Every user id the request names, with the role that id must have. */
    private static List<Claim> claims(HttpServletRequest request, String path) {
        List<Claim> claims = new ArrayList<>();
        HEADERS.forEach((name, role) -> {
            String value = request.getHeader(name);
            if (value != null) claims.add(new Claim(role, value));
        });
        PARAMETERS.forEach((name, role) -> {
            String value = request.getParameter(name);
            if (value != null) claims.add(new Claim(role, value));
        });
        for (Pattern pattern : APPLICANT_PATHS) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) claims.add(new Claim(Role.APPLICANT, matcher.group(1)));
        }
        return claims;
    }

    private boolean stillActiveWithSameRole(AuthenticatedUser user) {
        return accounts.findById(user.id())
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE && account.getRole() == user.role())
                .isPresent();
    }

    /** Writes the same {"message": ...} body as ApiError; the messages are fixed text, so no escaping is needed. */
    private static boolean reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
        return false;
    }

    /** A user id named by the request and the role that id must have. */
    private record Claim(Role role, String id) {
        boolean belongsTo(AuthenticatedUser user) {
            return user.role() == role && id.trim().equals(String.valueOf(user.id()));
        }
    }
}
