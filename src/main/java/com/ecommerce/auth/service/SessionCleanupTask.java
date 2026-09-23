package com.ecommerce.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupTask.class);

    private final SessionService sessionService;
    private final OtpService otpService;

    public SessionCleanupTask(SessionService sessionService, OtpService otpService) {
        this.sessionService = sessionService;
        this.otpService = otpService;
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanExpiredSessions() {
        int deleted = sessionService.cleanExpiredSessions();
        if (deleted > 0) {
            log.info("Cleaned up {} expired sessions", deleted);
        }
    }
}