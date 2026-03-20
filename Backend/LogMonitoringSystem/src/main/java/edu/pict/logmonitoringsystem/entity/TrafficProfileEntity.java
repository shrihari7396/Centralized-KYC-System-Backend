package edu.pict.logmonitoringsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "traffic_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficProfileEntity {
    @Id
    @Column(name = "`key`")
    private String key;

    private String ip;
    private String endpoint;

    private long requestCount;
    private long errorCount;

    private double avgPayloadSize;
    private double payloadEntropy;

    private double avgInterRequestGap;

    private Instant windowStart;
    private Instant windowEnd;
}
