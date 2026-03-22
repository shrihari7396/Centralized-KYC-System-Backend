package com.rbi.userservice.Repository;

import com.rbi.userservice.Model.UserEntity;
import com.rbi.userservice.Model.UserStatus;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    java.util.Optional<UserEntity> findByPhone(String phone);

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.lastLoginAt = CURRENT_TIMESTAMP WHERE u.email = :email")
    int updateLastLogin(@Param("email") String email);

    Optional<UserEntity> findByEmail(String email);

    UserStatus findUserStatusByEmail(String email);
}
