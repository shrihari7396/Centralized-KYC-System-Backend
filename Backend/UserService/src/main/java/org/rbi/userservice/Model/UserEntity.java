package org.rbi.userservice.Model;

import org.rbi.userservice.security.StringCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Builder
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Convert(converter = StringCryptoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String firstName;

    @Convert(converter = StringCryptoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lastName;

    @Convert(converter = StringCryptoConverter.class)
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String phone;

    private Role roles = Role.USER;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private LocalDateTime lastLoginAt;

    @CreationTimestamp private LocalDateTime createdAt;

    @UpdateTimestamp private LocalDateTime updatedAt;
}
