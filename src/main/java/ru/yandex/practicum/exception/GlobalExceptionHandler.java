package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
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

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(TypeMismatchException ex) {
        String paramName = ex.getPropertyName();
        String invalidValue = ex.getValue().toString();
        log.warn("Invalid parameter: {} with value: {}", paramName, invalidValue);
        String userMessage = String.format("Неверный формат параметра: %s со значением: %s", paramName, invalidValue);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(userMessage);
        return ResponseEntity.badRequest().body(errorResponseDto);
    }

    @ExceptionHandler(IdMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleIdMismatch(IdMismatchException ex) {
        log.warn(ex.getMessage());
        String userMessage = String.format(
                "Несовпадение идентификаторов %s: значение из пути - %s, а в теле запроса - %s",
                ex.getFieldName(), ex.getPathVariableId(), ex.getRequestBodyId()
        );
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(userMessage);
        return ResponseEntity.badRequest().body(errorResponseDto);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDto> handleGeneralDataAccess(DataAccessException ex) {
        int statusCode;
        String userMessage;

        if (ex instanceof DuplicateKeyException) {
            statusCode = HttpStatus.CONFLICT.value();
            userMessage = MSG_DUPLICATE_KEY_EXCEPTION;
            log.warn("Duplicate key exception", ex);
        } else if (ex instanceof DataIntegrityViolationException) {
            statusCode = HttpStatus.BAD_REQUEST.value();
            userMessage = MSG_DATA_INTEGRITY_VIOLATION_EXCEPTION;
            log.warn("Data integrity violation exception", ex);
        } else {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
            userMessage = MSG_INTERNAL_SERVER_ERROR;
            log.error("Internal server error: {}", ex.getMessage(), ex);
        }

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(userMessage);
        return ResponseEntity
                .status(statusCode)
                .body(errorResponseDto);
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlePostNotFound(PostNotFoundException ex) {
        log.warn(ex.getMessage());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(MSG_POST_NOT_FOUND);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponseDto);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleCommentNotFound(CommentNotFoundException ex) {
        log.warn(ex.getMessage());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(MSG_COMMENT_NOT_FOUND);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponseDto);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleImageNotFound(ImageNotFoundException ex) {
        log.warn(ex.getMessage());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(MSG_IMAGE_NOT_FOUND);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponseDto);
    }

    @ExceptionHandler(CommentCountUpdateException.class)
    public ResponseEntity<ErrorResponseDto> handleCommentCountUpdate(CommentCountUpdateException ex) {
        log.warn(ex.getMessage());
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(MSG_COMMENT_COUNT_UPDATE_ERROR);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponseDto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericError(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("TraceId: {} Unexpected error: {}", traceId, ex.getMessage(), ex);
        String userMessage = String.format("Произошла непредвиденная ошибка. TraceId: %s", traceId);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(userMessage);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponseDto);
    }
}
