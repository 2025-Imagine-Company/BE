package com.example.AudIon.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

record ApiError(String code, String message) {}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(500).body(new ApiError("INTERNAL_ERROR", e.getMessage()));
    }
}
