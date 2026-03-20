package edu.pict.logmonitoringsystem.repository;

import edu.pict.logmonitoringsystem.entity.LogEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<LogEntity, UUID> {}
