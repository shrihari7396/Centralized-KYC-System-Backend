package org.rbi.logmonitoringsystem.repository;

import org.rbi.logmonitoringsystem.entity.LogEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<LogEntity, UUID> {}
