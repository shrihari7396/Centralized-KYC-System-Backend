package edu.pict.logmonitoringsystem.repository;

import edu.pict.logmonitoringsystem.entity.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LogRepository extends JpaRepository<LogEntity, UUID> {
}
