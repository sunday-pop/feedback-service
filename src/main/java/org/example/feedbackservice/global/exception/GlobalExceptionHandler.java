package org.example.feedbackservice.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GitHubManagementException.class)
    public ResponseEntity<String> handleGitHubManagement(GitHubManagementException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotCompletedSummaryException.class)
    public ResponseEntity<String> handleNotCompletedSummary(NotCompletedSummaryException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FeedbackNotFoundException.class)
    public ResponseEntity<String> handleFeedbackNotFound(FeedbackNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}

