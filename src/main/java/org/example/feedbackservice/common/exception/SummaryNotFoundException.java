package org.example.feedbackservice.common.exception;

public class SummaryNotFoundException extends RuntimeException {
    public SummaryNotFoundException(String message) {
        super(message);
    }
}
