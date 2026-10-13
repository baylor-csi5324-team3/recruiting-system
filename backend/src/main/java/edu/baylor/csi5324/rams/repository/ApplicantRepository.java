package edu.baylor.csi5324.rams.repository;

import edu.baylor.csi5324.rams.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantRepository
        extends JpaRepository<Applicant, Long> {
}