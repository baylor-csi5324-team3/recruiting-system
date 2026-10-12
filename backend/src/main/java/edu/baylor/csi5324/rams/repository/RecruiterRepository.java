package edu.baylor.csi5324.rams.repository;

import edu.baylor.csi5324.rams.domain.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    Optional<Recruiter> findByAccountId(Long accountId);
}
