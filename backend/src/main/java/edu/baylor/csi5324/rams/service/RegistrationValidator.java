package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.RegisterAccountRequest;
import edu.baylor.csi5324.rams.domain.Role;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** Input validation only. Uniqueness, BCrypt and persistence belong to RegistrationService. */
@Component
public class RegistrationValidator {

    private final Validator validator;

    public RegistrationValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(RegisterAccountRequest request) {
        if (request == null) {
            throw new ValidationException("Provide registration information.");
        }
        String errors = validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .sorted()
                .distinct()
                .collect(Collectors.joining(" "));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (request.role() != Role.APPLICANT && request.role() != Role.RECRUITER) {
            throw new ValidationException("Public registration supports APPLICANT or RECRUITER only.");
        }
        if (request.password().codePointCount(0, request.password().length()) < 8) {
            throw new ValidationException("Password must contain at least 8 Unicode characters.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Password must not exceed 72 UTF-8 bytes.");
        }
        // The full name is stored with the applicant or recruiter; it is not required to be unique.
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new ValidationException("Provide your full name.");
        }
        if (request.fullName().codePointCount(0, request.fullName().length()) > 100) {
            throw new ValidationException("Full name must not exceed 100 Unicode characters.");
        }
        if (request.role() == Role.RECRUITER
                && (request.companyName() == null || request.companyName().isBlank())) {
            throw new ValidationException("Provide a company name for recruiter registration.");
        }
    }
}
