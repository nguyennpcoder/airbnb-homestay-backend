package com.project.airbnb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("message", "Dữ liệu không hợp lệ");
        response.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        String message = ex.getMessage();
        response.put("message", message != null ? message : "Có lỗi xảy ra");

        // Detect locked account error → return 403 with locked flag
        if (message != null && message.toLowerCase().contains("khóa")) {
            response.put("locked", true);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Final fallback — any uncaught exception (including JDBC/DB errors) returns
     * a JSON body with a clear message instead of an empty 500 page. Frontend
     * can then show the real cause instead of a generic "Có lỗi xảy ra".
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAnyException(Exception ex) {
        log.error("Unhandled exception", ex);
        Map<String, Object> response = new HashMap<>();
        String root = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        response.put("message", "Lỗi hệ thống: " + root);
        response.put("type", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

