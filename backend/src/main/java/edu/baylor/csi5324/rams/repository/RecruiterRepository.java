package edu.baylor.csi5324.rams.repository;

import edu.baylor.csi5324.rams.domain.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
}
