// package com.rbi.userservice.controller;

// import static org.mockito.Mockito.*;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.rbi.userservice.DTO.OtpRequestDto;
// import com.rbi.userservice.DTO.OtpVerificationDto;
// import com.rbi.userservice.DTO.RegisterDTO;
// import com.rbi.userservice.DTO.UserProfileDto;
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
//     void testRequestOTPByPhone_Success() throws Exception {
//         when(userService.sendOtpByPhone("1234567890")).thenReturn(true);

//         mockMvc.perform(post("/user/request-otp-phone").param("phone", "1234567890"))
//                 .andExpect(status().isCreated())
//                 .andExpect(content().string("OTP Sent successfully to 1234567890"));
//     }

//     @Test
//     void testOtpVerification_Success() throws Exception {
//         OtpVerificationDto dto = new OtpVerificationDto();
//         dto.setOtp("123456");

//         UserProfileDto responseDto =
//                 new UserProfileDto(); // Using any suitable DTO structure that UserProfile
// returns
//         // getLoginResponseDto returns LoginResponseDto, let's adjust mock
//         when(userService.getLoginResponseDto(any(OtpVerificationDto.class)))
//                 .thenReturn(null); // Just verify binds

//         mockMvc.perform(
//                         post("/user/otp-verification")
//                                 .contentType(MediaType.APPLICATION_JSON)
//                                 .content(objectMapper.writeValueAsString(dto)))
//                 .andExpect(status().isCreated());
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
// }
