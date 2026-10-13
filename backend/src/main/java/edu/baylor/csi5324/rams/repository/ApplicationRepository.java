package edu.baylor.csi5324.rams.repository;

import edu.baylor.csi5324.rams.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {
}