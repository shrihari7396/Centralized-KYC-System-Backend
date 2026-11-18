package org.rbi.userservice.Repository;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.rbi.userservice.Model.UserEntity;
import org.rbi.userservice.Model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByPhone(String phone);

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.lastLoginAt = CURRENT_TIMESTAMP WHERE u.email = :email")
    int updateLastLogin(@Param("email") String email);

    Optional<UserEntity> findByEmail(String email);

    @Query("SELECT u.status FROM UserEntity u WHERE u.email = :email")
    UserStatus findUserStatusByEmail(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.status = :status WHERE LOWER(u.email) = LOWER(:email)")
    int updateStatusByEmail(@Param("email") String email, @Param("status") UserStatus status);
}
