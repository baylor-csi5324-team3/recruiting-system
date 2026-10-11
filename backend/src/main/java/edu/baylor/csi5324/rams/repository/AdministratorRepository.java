package edu.baylor.csi5324.rams.repository;

import edu.baylor.csi5324.rams.domain.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, Long> {
}
