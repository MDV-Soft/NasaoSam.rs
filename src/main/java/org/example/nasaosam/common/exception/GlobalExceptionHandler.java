package org.example.nasaosam.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // validacija podataka iz forme
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors;
        List<FieldError> errors;
        ApiError body;

        fieldErrors = new LinkedHashMap<>();
        errors = exception.getBindingResult().getFieldErrors();

        for (int i = 0; i < errors.size(); i++) {
            FieldError error = errors.get(i);

            if (!fieldErrors.containsKey(error.getField())) {
                fieldErrors.put(error.getField(), error.getDefaultMessage());
            }
        }

        body = new ApiError(HttpStatus.BAD_REQUEST.value(), "Neispravni podaci", Instant.now(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exc) {
        ApiError body;

        body = new ApiError(HttpStatus.BAD_REQUEST.value(), "Neisprava format zahteva", Instant.now(), Map.of());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    //greske u servisima
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exc) {
        int statusCode;
        String message;
        ApiError body;

        statusCode = exc.getStatusCode().value();
        message = exc.getReason();
        if (message == null) {
            message = "Greska";
        }

        body = new ApiError(statusCode, message, Instant.now(), Map.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 403 vs 500 - AUTORIZACIJA
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exc) {
        ApiError body;

        body = new ApiError(HttpStatus.FORBIDDEN.value(), "Nemate dozvoljen pristup", Instant.now(), Map.of());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // log fajl
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception){
        ApiError body;

        logger.error("Neocekivana greska", exception);
        body = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Doslo je do greske na serveru", Instant.now(), Map.of());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}