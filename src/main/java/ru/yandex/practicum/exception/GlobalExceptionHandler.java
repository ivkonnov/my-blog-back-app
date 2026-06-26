package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.dto.ErrorResponseDto;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ru.yandex.practicum.exception.ErrorMessages.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String field = error.getField().replaceAll("\\[\\d+\\]", "");
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        log.warn("ValidationError: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDto> handleGeneralDataAccess(DataAccessException ex) {
        int statusCode;
        String message;

        if (ex instanceof DuplicateKeyException) {
            statusCode = HttpStatus.CONFLICT.value();
            message = MSG_DUPLICATE_KEY_EXCEPTION;
            log.warn("Duplicate key exception", ex);
        } else if (ex instanceof DataIntegrityViolationException) {
            statusCode = HttpStatus.BAD_REQUEST.value();
            message = MSG_DATA_INTEGRITY_VIOLATION_EXCEPTION;
            log.warn("Data integrity violation exception", ex);
        } else {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
            message = MSG_INTERNAL_SERVER_ERROR;
            log.error("Internal server error: {}", ex.getMessage(), ex);
        }

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(message);
        return ResponseEntity
                .status(statusCode)
                .body(errorResponseDto);
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlePostNotFound(PostNotFoundException ex) {
        log.warn("Post not found with postId: {}", ex.getPostId());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponseDto);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleCommentNotFound(CommentNotFoundException ex) {
        log.warn("Comment with id {} for post with id {} not found", ex.getCommentId(), ex.getPostId());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponseDto);
    }

    @ExceptionHandler(ImagePostNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleImagePostNotFound(ImagePostNotFoundException ex) {
        log.warn("Image for post with id {} not found", ex.getPostId());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponseDto);
    }

    @ExceptionHandler(CommentCountUpdateException.class)
    public ResponseEntity<ErrorResponseDto> handlePostNotFound(CommentCountUpdateException ex) {
        log.warn("Couldn't update comment counter for post with id {}", ex.getPostId());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponseDto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericError(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("TraceId: {} Unexpected error: {}", traceId, ex.getMessage(), ex);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                "Произошла непредвиденная ошибка. TraceId: " + traceId
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponseDto);
    }
}
