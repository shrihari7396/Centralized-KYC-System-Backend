package org.kyc.management.bankservice.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "banks")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bank {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String name;
  private String code;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Timestamp createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = new Timestamp(System.currentTimeMillis());
  }
}
