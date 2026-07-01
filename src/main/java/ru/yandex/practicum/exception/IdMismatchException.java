package ru.yandex.practicum.exception;

import lombok.Getter;

@Getter
public class IdMismatchException extends RuntimeException {

    private final String fieldName;

    private final Long pathVariableId;

    private final Long requestBodyId;

    public IdMismatchException(String fieldName, Long pathVariableId, Long requestBodyId) {
        super(String.format("Id mismatch for %s: path variable: %s and in request body: %s are different", fieldName, pathVariableId, requestBodyId));
        this.fieldName = fieldName;
        this.pathVariableId = pathVariableId;
        this.requestBodyId = requestBodyId;
    }
}
