package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.Session;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.exception.ApiException;
import com.ecommerce.auth.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public Session createSession(User user, String token, LocalDateTime expiry) {
        Session session = Session.builder()
                .user(user)
                .token(token)
                .loginTime(LocalDateTime.now())
                .expiryTime(expiry)
                .build();
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Session findByToken(String token) {
        return sessionRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired session. Please log in again."));
    }

    @Transactional
    public void logout(String token) {
        sessionRepository.findByToken(token).ifPresent(sessionRepository::delete);
    }

    @Transactional
    public void invalidateAllUserSessions(Long userId) {
        sessionRepository.deleteByUserId(userId);
    }

    @Transactional
    public void invalidateOtherSessions(Long userId, String currentToken) {
        sessionRepository.deleteOtherSessions(userId, currentToken);
    }

    @Transactional
    public int cleanExpiredSessions() {
        return sessionRepository.deleteExpired(LocalDateTime.now());
    }
}