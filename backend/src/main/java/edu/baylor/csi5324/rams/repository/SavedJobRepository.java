
package edu.baylor.csi5324.rams.repository;

import edu.baylor.csi5324.rams.domain.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedJobRepository
        extends JpaRepository<SavedJob, Long> {

    boolean existsByApplicantIdAndJobPostingId(
            Long applicantId, Long jobPostingId);

    List<SavedJob> findByApplicantIdOrderBySavedAtDesc(
            Long applicantId);

    void deleteByApplicantIdAndJobPostingId(
            Long applicantId, Long jobPostingId);
}
