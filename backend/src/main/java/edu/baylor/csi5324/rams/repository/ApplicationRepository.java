package edu.baylor.csi5324.rams.repository;

import edu.baylor.csi5324.rams.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByApplicantId(Long applicantId);

    List<Application> findByJobPosting_Company_IdOrderBySubmitDateDesc(Long companyId);

    boolean existsByApplicantIdAndJobPostingId(Long applicantId, Long jobPostingId);

    List<Application> findByApplicantIdOrderBySubmitDateDesc(Long applicantId);
}