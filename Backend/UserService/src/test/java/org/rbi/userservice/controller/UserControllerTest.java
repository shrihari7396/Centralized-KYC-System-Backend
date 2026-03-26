// package com.rbi.userservice.controller;

// import static org.mockito.Mockito.*;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.rbi.userservice.DTO.LoginResponseDto;
// import com.rbi.userservice.DTO.OtpRequestDto;
// import com.rbi.userservice.DTO.OtpVerificationDto;
// import com.rbi.userservice.DTO.RegisterDTO;
// import com.rbi.userservice.DTO.UserDto;
// import com.rbi.userservice.Model.UserStatus;
// import com.rbi.userservice.kafkaEvents.LogProducer;
// import com.rbi.userservice.service.UserService;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;

// @WebMvcTest(UserController.class)
// @AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
// public class UserControllerTest {

//     @Autowired private MockMvc mockMvc;
//     @Autowired private ObjectMapper objectMapper;

//     @MockBean private UserService userService;
//     @MockBean private LogProducer logProducer;

//     @Test
//     void testRegisterUser_Success() throws Exception {
//         RegisterDTO dto = new RegisterDTO();
//         dto.setFirstName("John");
//         dto.setEmail("john@example.com");

//         when(userService.registerUser(any(RegisterDTO.class))).thenReturn(true);

//         mockMvc.perform(
//                         post("/user/register")
//                                 .contentType(MediaType.APPLICATION_JSON)
//                                 .content(objectMapper.writeValueAsString(dto)))
//                 .andExpect(status().isCreated())
//                 .andExpect(content().string("User registered successfully ..."));
//     }

//     @Test
//     void testRequestOTP_Success() throws Exception {
//         OtpRequestDto req = new OtpRequestDto();
//         req.setEmail("john@example.com");

//         when(userService.isExistByEmail("john@example.com")).thenReturn(true);
//         when(userService.sendOtp(any(OtpRequestDto.class))).thenReturn(true);

//         mockMvc.perform(
//                         post("/user/request-otp")
//                                 .contentType(MediaType.APPLICATION_JSON)
//                                 .content(objectMapper.writeValueAsString(req)))
//                 .andExpect(status().isCreated())
//                 .andExpect(content().string("OTP Sent successfully ..."));
//     }

//     @Test
//     void testRequestOTP_UserNotFound() throws Exception {
//         OtpRequestDto req = new OtpRequestDto();
//         req.setEmail("unknown@example.com");

//         when(userService.isExistByEmail("unknown@example.com")).thenReturn(false);

//         mockMvc.perform(
//                         post("/user/request-otp")
//                                 .contentType(MediaType.APPLICATION_JSON)
//                                 .content(objectMapper.writeValueAsString(req)))
//                 .andExpect(
//                         status().isNotFound()); // UserNotFoundException should be handled or
// throw
//         // 404 depending on Advice
//     }

//     @Test
//     void testOtpVerification_Success() throws Exception {
//         OtpVerificationDto dto = new OtpVerificationDto();
//         dto.setOtp("123456");
//         dto.setEmail("john@example.com");

//         LoginResponseDto responseDto =
//
// LoginResponseDto.builder().email("john@example.com").token("valid_token").build();

//         when(userService.getLoginResponseDto(any(OtpVerificationDto.class)))
//                 .thenReturn(responseDto);

//         mockMvc.perform(
//                         post("/user/otp-verification")
//                                 .contentType(MediaType.APPLICATION_JSON)
//                                 .content(objectMapper.writeValueAsString(dto)))
//                 .andExpect(status().isCreated())
//                 .andExpect(jsonPath("$.token").value("valid_token"));
//     }

//     @Test
//     void testLogout_Success() throws Exception {
//         when(userService.logout("valid_token")).thenReturn(true);

//         mockMvc.perform(post("/user/logout").header("Authorization", "Bearer valid_token"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().string("Logged out successfully"));
//     }

//     @Test
//     void testLogout_InvalidHeader() throws Exception {
//         mockMvc.perform(post("/user/logout").header("Authorization", "invalid_header"))
//                 .andExpect(status().isBadRequest())
//                 .andExpect(content().string("Invalid Authorization header"));
//     }

//     @Test
//     void testGetUserDetails_Success() throws Exception {
//         UserDto userDto =
//                 UserDto.builder()
//                         .userId("uuid-123")
//                         .firstName("John")
//                         .lastName("Doe")
//                         .email("john@example.com")
//                         .phone("1234567890")
//                         .status(UserStatus.KYC_PENDING)
//                         .build();

//         when(userService.getUserDetails("john@example.com")).thenReturn(userDto);

//         mockMvc.perform(get("/user/details").param("email", "john@example.com"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.email").value("john@example.com"))
//                 .andExpect(jsonPath("$.firstName").value("John"));
//     }

//     @Test
//     void testGetStatusByEmail_Success() throws Exception {
//         when(userService.getUserStatusByEmail("john@example.com"))
//                 .thenReturn(UserStatus.KYC_PENDING);

//         mockMvc.perform(get("/user/get-kyc-status/john@example.com"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().string("\"KYC_PENDING\""));
//     }
// }
