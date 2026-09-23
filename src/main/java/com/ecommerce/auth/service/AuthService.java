package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.request.ChangePasswordRequest;
import com.ecommerce.auth.dto.request.ForgotPasswordRequest;
import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.request.ResetPasswordRequest;
import com.ecommerce.auth.dto.request.VerifyOtpRequest;
import com.ecommerce.auth.dto.response.ApiResponse;
import com.ecommerce.auth.dto.response.AuthResponse;
import com.ecommerce.auth.dto.response.UserResponse;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.exception.ApiException;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final OtpService otpService;
    private final MailService mailService;

    @Value("${app.otp.debug-return-otp:false}")
    private boolean debugReturnOtp;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SessionService sessionService,
                       OtpService otpService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.otpService = otpService;
        this.mailService = mailService;
    }

    @Transactional
    public ApiResponse<UserResponse> register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String mobile = request.getMobile().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password and confirm password do not match");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        if (userRepository.existsByMobile(mobile)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this mobile number already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .mobile(mobile)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);
        return ApiResponse.ok("Registration successful. Please log in.", UserResponse.from(user));
    }

    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = findByIdentifier(identifier)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile or password");
        }

        String token = jwtService.generateToken(user);
        LocalDateTime expiry = LocalDateTime.ofInstant(jwtService.extractExpiration(token).toInstant(), ZoneId.systemDefault());
        sessionService.createSession(user, token, expiry);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);
        authResponse.setExpiresAt(expiry);
        authResponse.setUser(UserResponse.from(user));

        return ApiResponse.ok("Login successful", authResponse);
    }

    @Transactional
    public ApiResponse<Void> logout(String token) {
        sessionService.logout(token);
        return ApiResponse.ok("Logged out successfully");
    }

    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = findByIdentifier(identifier)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        String otp = otpService.generateOtp(user.getEmail());
        mailService.sendOtpEmail(user.getEmail(), otp);
        if (debugReturnOtp) {
            return ApiResponse.ok("OTP has been sent to your registered email. (Dev mode OTP: %s)".formatted(otp));
        }
        return ApiResponse.ok("OTP has been sent to your registered email");
    }

    @Transactional
    public ApiResponse<Void> verifyOtp(VerifyOtpRequest request) {
        String identifier = request.getIdentifier().trim();
        findByIdentifier(identifier)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        otpService.verifyOtp(resolveIdentifier(identifier), request.getOtp());
        return ApiResponse.ok("OTP verified successfully. You can now reset your password.");
    }

    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = findByIdentifier(identifier)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));

        otpService.validateVerifiedOtp(resolveIdentifier(identifier), request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        sessionService.invalidateAllUserSessions(user.getId());
        otpService.cleanUp(resolveIdentifier(identifier), LocalDateTime.now());
        return ApiResponse.ok("Password reset successfully. Please log in with your new password.");
    }

    @Transactional
    public ApiResponse<UserResponse> changePassword(Long userId, String currentToken, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        sessionService.invalidateOtherSessions(user.getId(), currentToken);
        return ApiResponse.ok("Password changed successfully", UserResponse.from(user));
    }

    private Optional<User> findByIdentifier(String identifier) {
        if (identifier.matches("^\\d{10}$")) {
            return userRepository.findByMobile(identifier);
        }
        return userRepository.findByEmail(identifier.toLowerCase());
    }

    private String resolveIdentifier(String identifier) {
        return findByIdentifier(identifier.trim()).map(User::getEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
    }
}