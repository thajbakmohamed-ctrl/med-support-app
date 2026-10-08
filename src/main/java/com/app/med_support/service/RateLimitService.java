package com.app.med_support.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final int MAX_ATTEMPTS = 3;
    private static final int WINDOW_MINUTES = 1;
    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public boolean isAllowed(String key) {

        LocalDateTime now = LocalDateTime.now();
        AttemptInfo attemptInfo = attempts.get(key);

        if (attemptInfo == null) {
            attempts.put(key, new AttemptInfo(1, now));
            return true;
        }

        if (attemptInfo.startTime.plusMinutes(WINDOW_MINUTES).isBefore(now)) {
            attempts.put(key, new AttemptInfo(1, now));
            return true;
        }

        if (attemptInfo.count >= MAX_ATTEMPTS) {
            return false;
        }

        attemptInfo.count++;
        return true;
    }

    private static class AttemptInfo {

        private int count;
        private final LocalDateTime startTime;

        public AttemptInfo(int count, LocalDateTime startTime) {
            this.count = count;
            this.startTime = startTime;
        }
    }
}
