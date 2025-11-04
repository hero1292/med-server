package org.aleksanyan.medserver.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex) {
        return ResponseEntity
                .status(ex.getCode().getStatus())
                .body(Map.of("error", ex.getCode().name(), "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                e -> e.getField(),
                                e -> e.getDefaultMessage(),
                                (a, b) -> a)
                );
        return ResponseEntity.badRequest().body(Map.of("validationErrors", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex) {
        return ResponseEntity
                .status(500)
                .body(Map.of("error", "INTERNAL_ERROR", "message", ex.getMessage()));
    }
}
