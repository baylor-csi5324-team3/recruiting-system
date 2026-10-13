package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Application;
import edu.baylor.csi5324.rams.domain.ApplicationStage;
import edu.baylor.csi5324.rams.domain.ApplicationSummary;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository applications;
    private final ApplicantRepository applicants;
    private final Clock clock;

    public ApplicationService(
            ApplicationRepository applications,
            ApplicantRepository applicants,
            Clock clock) {

        this.applications = applications;
        this.applicants = applicants;
        this.clock = clock;
    }

    // 对应顺序图：getOfferedApplication() → getDetails()。
    @Transactional(readOnly = true)
    public ApplicationSummary getOfferedApplication(
            Long applicationId,
            Long applicantId) {

        Application application =
                findOwnedApplication(applicationId, applicantId);

        return application.getDetails();
    }

    // Applicant 确认接受或拒绝后，更新申请的 Stage。
    public ApplicationSummary updateStage(
            Long applicationId,
            Long applicantId,
            String decision) {

        // 先校验输入，避免将非法值当成 DECLINE。
        if (!"ACCEPT".equals(decision)
                && !"DECLINE".equals(decision)) {
            throw new ValidationException(
                    "Decision must be ACCEPT or DECLINE."
            );
        }

        Application application =
                findOwnedApplication(applicationId, applicantId);

        if (application.getStage() != ApplicationStage.OFFER) {
            throw new ValidationException(
                    "Only an application in OFFER stage can be accepted or declined."
            );
        }

        ApplicationStage nextStage = "ACCEPT".equals(decision)
                ? ApplicationStage.ACCEPTED
                : ApplicationStage.DECLINED;

        // 对应顺序图：recordStage(ACCEPTED / DECLINED)。
        application.recordStage(
                nextStage,
                LocalDateTime.now(clock)
        );

        applications.saveAndFlush(application);

        return application.getDetails();
    }

    // 查询申请，同时校验账号状态和申请归属。
    private Application findOwnedApplication(
            Long applicationId,
            Long applicantId) {

        Applicant applicant = applicants.findById(applicantId)
                .orElseThrow(() -> new NotFoundException(
                        "No applicant with id " + applicantId + "."
                ));

        UserAccount account = applicant.getAccount();

        if (account == null
                || account.getRole() != Role.APPLICANT
                || account.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException(
                    "An active applicant account is required."
            );
        }

        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException(
                        "No application with id " + applicationId + "."
                ));

        if (!application.belongsTo(applicant)) {
            throw new ValidationException(
                    "You can only access your own application."
            );
        }

        return application;
    }
}