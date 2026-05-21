package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error -> {
                String field = error.getField().replaceAll("\\[\\d+\\]", "");;
                String message = error.getDefaultMessage();
                errors.put(field, message);
            }
        );
        log.warn("Validation error: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<String> handleGeneralDataAccess(DataAccessException ex) {
        String userMessage = switch (ex) {
            case DuplicateKeyException dke -> "Запись с такими данными уже существует";
            case DataIntegrityViolationException dive -> "Нарушение целостности данных";
            default -> "Произошла ошибка при работе с базой данных";
        };
        log.error("Database error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(userMessage);
    }
}
