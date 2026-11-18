package org.rbi.userservice.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.rbi.userservice.security.StringCryptoConverter;

@Builder
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Convert(converter = StringCryptoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String firstName;

    @Convert(converter = StringCryptoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lastName;

    @Convert(converter = StringCryptoConverter.class)
    @Column(nullable = false, length = 255)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private Role roles = Role.USER;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
