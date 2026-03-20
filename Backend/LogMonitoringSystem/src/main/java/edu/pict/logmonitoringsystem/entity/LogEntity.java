package edu.pict.logmonitoringsystem.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String serviceId;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private LogLevel level;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant ingestedAt;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID();
        this.ingestedAt = Instant.now();
    }
}
