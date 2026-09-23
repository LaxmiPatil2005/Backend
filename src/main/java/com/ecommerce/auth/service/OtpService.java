package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.Otp;
import com.ecommerce.auth.exception.ApiException;
import com.ecommerce.auth.repository.OtpRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALID_MINUTES = 10;

    private final SecureRandom secureRandom = new SecureRandom();
    private final OtpRepository otpRepository;

    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Transactional
    public String generateOtp(String identifier) {
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        Otp otp = Otp.builder()
                .identifier(identifier.toLowerCase())
                .code(code)
                .expiryTime(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES))
                .verified(false)
                .build();
        otpRepository.save(otp);
        return code;
    }

    @Transactional
    public void verifyOtp(String identifier, String code) {
        String normalized = identifier.toLowerCase();
        Otp otp = otpRepository.findTopByIdentifierOrderByIdDesc(normalized)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid OTP. Please request a new one."));

        if (otp.isVerified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This OTP has already been used. Please request a new one.");
        }
        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP has expired. Please request a new one.");
        }
        if (!otp.getCode().equals(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect OTP. Please try again.");
        }

        otp.setVerified(true);
        otpRepository.save(otp);
    }

    @Transactional
    public void validateVerifiedOtp(String identifier, String code) {
        String normalized = identifier.toLowerCase();
        Otp otp = otpRepository.findTopByIdentifierOrderByIdDesc(normalized)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No OTP found. Please request a new one."));

        if (!otp.isVerified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please verify your OTP before resetting the password.");
        }
        if (!otp.getCode().equals(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect OTP. Please restart the password reset process.");
        }
        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP has expired. Please request a new one.");
        }
    }

    @Transactional
    public void cleanUp(String identifier, LocalDateTime now) {
        otpRepository.deleteByIdentifier(identifier.toLowerCase());
        otpRepository.deleteStale(now);
    }
}