package org.rbi.logmonitoringsystem.repository;

import java.util.UUID;
import org.rbi.logmonitoringsystem.entity.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<LogEntity, UUID> {}
