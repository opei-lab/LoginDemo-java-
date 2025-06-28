package com.example.demo.exception;

/**
 * レート制限例外
 */
public class RateLimitException extends RuntimeException {
    private final int retryAfterSeconds;
    
    public RateLimitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}