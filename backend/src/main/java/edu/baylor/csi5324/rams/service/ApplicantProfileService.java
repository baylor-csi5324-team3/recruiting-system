package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.ApplicantProfile;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC 4.2.1.4 Update Applicant Profile. Service lifeline of SD 6.4: messages 1.1 getProfile and 5 saveProfile. */
@Service
@Transactional
public class ApplicantProfileService {

    private final ApplicantRepository applicants;

    public ApplicantProfileService(ApplicantRepository applicants) {
        this.applicants = applicants;
    }

    /** SD 6.4 message 1.1: the current profile of the identified applicant. */
    @Transactional(readOnly = true)
    public ApplicantProfile getProfile(Long applicantId) {
        return activeApplicant(applicantId).getDetails();                    // 1.1.1, 1.1.2
    }

    /** SD 6.4 message 5: saves already validated changes and returns the saved profile. */
    public ApplicantProfile saveProfile(ApplicantProfile updatedInfo, Long applicantId) {
        Applicant applicant = activeApplicant(applicantId);                   // 5.1
        applicant.updateDetails(updatedInfo);                                 // 5.2
        return applicants.saveAndFlush(applicant).getDetails();               // 5.3
    }

    private Applicant activeApplicant(Long applicantId) {
        Applicant applicant = applicants.findById(applicantId)
                .orElseThrow(() -> new NotFoundException("No applicant with id " + applicantId + "."));
        if (applicant.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("An active applicant account is required.");
        }
        return applicant;
    }
}
