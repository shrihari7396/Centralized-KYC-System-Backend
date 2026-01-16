package com.rbi.userservice.service;

import com.rbi.userservice.DTO.UserDTO;
import org.springframework.http.ResponseEntity;

public interface UserServiceInterface {
    Boolean userLogin(String email);
    Boolean uegisterUser(UserDTO user);
}
