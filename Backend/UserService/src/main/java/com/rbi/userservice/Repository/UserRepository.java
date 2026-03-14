package com.rbi.userservice.Repository;

import com.rbi.userservice.Model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    UserEntity findByEmail(String email);
    java.util.Optional<UserEntity> findByPhone(String phone);
}
