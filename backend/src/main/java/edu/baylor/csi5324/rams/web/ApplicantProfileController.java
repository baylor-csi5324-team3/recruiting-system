package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.ApplicantProfile;
import edu.baylor.csi5324.rams.service.ApplicantProfileService;
import edu.baylor.csi5324.rams.service.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC 4.2.1.4 Update Applicant Profile. Controller lifeline of SD 6.4:
 * 1 viewProfile, 1.1 getProfile, 3 submitProfile, 4 validate, 5 saveProfile.
 * Like the other applicant end points, the applicant is identified by the {@code X-Applicant-Id} header.
 */
@RestController
@RequestMapping("/api/profile")
public class ApplicantProfileController {

    private static final int MAX_NAME = 100;
    private static final int MAX_PHONE = 30;
    private static final int MAX_LOCATION = 100;
    private static final int MAX_HEADLINE = 150;
    private static final int MAX_BIO = 2000;

    private final ApplicantProfileService profileService;

    public ApplicantProfileController(ApplicantProfileService profileService) {
        this.profileService = profileService;
    }

    /** Steps 1 and 2: the applicant's current profile. */
    @GetMapping
    public ApplicantProfile viewProfile(@RequestHeader(ApplicationController.APPLICANT_HEADER) Long applicantId) {
        return profileService.getProfile(applicantId);                       // 1.1
    }

    /** Steps 3 to 6: validates and saves the changes, then reports the saved profile. */
    @PutMapping
    public ApplicantProfile submitProfile(@RequestHeader(ApplicationController.APPLICANT_HEADER) Long applicantId,
                                          @RequestBody ProfileUpdate updatedInfo,
                                          HttpServletRequest request) {
        ApplicantProfile valid = validate(updatedInfo, applicantId);         // 4
        ApplicantProfile saved = profileService.saveProfile(valid, applicantId);   // 5
        CurrentUser.renameApplicant(request, applicantId, saved.name());
        return saved;
    }

    /** Step 4, extension 4a: the name is required; the other fields are optional and limited in length. */
    private ApplicantProfile validate(ProfileUpdate updatedInfo, Long applicantId) {
        String name = trim(updatedInfo.name());
        if (name == null) {
            throw new ValidationException("Provide your full name.");
        }
        if (name.codePointCount(0, name.length()) > MAX_NAME) {
            throw new ValidationException("Full name must not exceed " + MAX_NAME + " characters.");
        }
        String phone = trim(updatedInfo.phone());
        if (phone != null && (phone.length() > MAX_PHONE || !phone.matches("[0-9+()\\-. ]*"))) {
            throw new ValidationException("Provide a phone number of up to " + MAX_PHONE
                    + " characters using digits, spaces and + ( ) - .");
        }
        return new ApplicantProfile(applicantId, name, null, phone,
                limited(updatedInfo.location(), MAX_LOCATION, "Location"),
                limited(updatedInfo.headline(), MAX_HEADLINE, "Professional headline"),
                limited(updatedInfo.bio(), MAX_BIO, "About you"));
    }

    private static String limited(String value, int max, String field) {
        String text = trim(value);
        if (text != null && text.codePointCount(0, text.length()) > max) {
            throw new ValidationException(field + " must not exceed " + max + " characters.");
        }
        return text;
    }

    /** Blank text is stored as no value. */
    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Step 3: the information the applicant may change; the email identifies the account and is not changed here. */
    public record ProfileUpdate(String name, String phone, String location, String headline, String bio) {
    }
}
