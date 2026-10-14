package edu.baylor.csi5324.rams.repository;

import edu.baylor.csi5324.rams.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Retrieves resumes belonging to the applicant submitting the application. */
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByApplicantIdOrderByIdDesc(Long applicantId);
}
