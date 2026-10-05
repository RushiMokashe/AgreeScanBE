package com.myagree.app.common;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.i18n.UserMessage;

/**
 * Translates every failure into the {@link ApiError} body the frontend expects. Messages for the user are given in
 * the request's language; messages about malformed requests, which only a developer sees, stay in English. Stack
 * traces never leave the server.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final UserMessage UNEXPECTED_ERROR = UserMessage.of("common.error.unexpected");
    private static final UserMessage UPLOAD_TOO_LARGE = UserMessage.of("common.error.upload-too-large");
    private static final String UNREADABLE_BODY_MESSAGE = "Request body is missing or is not valid JSON";
    private static final String MESSAGE_SEPARATOR = "; ";

    private final Messages messages;

    ApiExceptionHandler(Messages messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException ex, Language language) {
        return respond(ex.status(), ex.userMessage(messages, language));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(ApiExceptionHandler::describe)
                .collect(Collectors.joining(MESSAGE_SEPARATOR));
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleInvalidParameters(HandlerMethodValidationException ex) {
        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + " " + error.getDefaultMessage()))
                .collect(Collectors.joining(MESSAGE_SEPARATOR));
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '%s' for '%s'%s".formatted(ex.getValue(), ex.getName(), allowedValues(ex.getRequiredType()));
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, UNREADABLE_BODY_MESSAGE);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException ex, Language language) {
        return respond(HttpStatus.CONTENT_TOO_LARGE, messages.get(UPLOAD_TOO_LARGE, language));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, Language language) {
        // Spring MVC's own exceptions (unknown route, wrong method, missing parameter, ...) carry their status.
        if (ex instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .headers(errorResponse.getHeaders())
                    .body(ApiError.of(errorResponse.getStatusCode(), detailOf(errorResponse)));
        }
        log.error("Unhandled error while serving an API request", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, messages.get(UNEXPECTED_ERROR, language));
    }

    private static ResponseEntity<ApiError> respond(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, message));
    }

    private static String describe(MessageSourceResolvable error) {
        return error instanceof FieldError fieldError
                ? fieldError.getField() + " " + fieldError.getDefaultMessage()
                : String.valueOf(error.getDefaultMessage());
    }

    private static String allowedValues(@Nullable Class<?> type) {
        if (type == null || !type.isEnum()) {
            return "";
        }
        return Arrays.stream(type.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", ", ". Allowed values: ", ""));
    }

    private static String detailOf(ErrorResponse errorResponse) {
        String detail = errorResponse.getBody().getDetail();
        return detail != null ? detail : HttpStatus.valueOf(errorResponse.getStatusCode().value()).getReasonPhrase();
    }
}
