package edu.pict.logmonitoringsystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threat_assessments")
public class ThreatAssessment {

    @Id
    private UUID id;

    private String sourceKey; // ip:endpoint

    private double threatScore;

    private String classification; // NORMAL / SUSPICIOUS / ATTACK

    private String decision; // RATE_LIMIT / BLOCK / ALLOW

    private Instant evaluatedAt;
}
