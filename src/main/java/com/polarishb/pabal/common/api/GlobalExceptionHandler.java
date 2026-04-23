package com.polarishb.pabal.common.api;

import com.polarishb.pabal.common.exception.GlobalException;
import com.polarishb.pabal.common.exception.code.CommonErrorCode;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiError> handleGlobalException(GlobalException e, HttpServletRequest request) {
        HttpStatusCode statusCode = HttpStatusCode.valueOf(e.getErrorCode().getHttpStatus());
        ApiError apiError = ApiError.of(
                statusCode,
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage(),
                request.getRequestURI(),
                currentTraceId()
        );
        logApiError(e, apiError, e.getPayload());

        return ResponseEntity
                .status(statusCode)
                .body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception e, HttpServletRequest request) {
        HttpExceptionResponse httpExceptionResponse = resolveHttpExceptionResponse(e);
        if (httpExceptionResponse != null
                && httpExceptionResponse.statusCode().value() != HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            ApiError apiError = toApiError(
                    httpExceptionResponse.statusCode(),
                    request.getRequestURI(),
                    currentTraceId(),
                    validationDetails(e)
            );
            logApiError(e, apiError);

            return ResponseEntity
                    .status(httpExceptionResponse.statusCode())
                    .headers(httpExceptionResponse.headers())
                    .body(apiError);
        }

        ApiError apiError = toApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                currentTraceId()
        );
        logApiError(e, apiError);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiError);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        ApiError apiError = toApiError(
                statusCode,
                requestPath(request),
                currentTraceId(),
                validationDetails(ex)
        );
        logApiError(ex, apiError);
        return super.handleExceptionInternal(ex, apiError, headers, statusCode, request);
    }

    private HttpExceptionResponse resolveHttpExceptionResponse(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse errorResponse) {
            return new HttpExceptionResponse(
                    errorResponse.getStatusCode(),
                    errorResponse.getHeaders()
            );
        }

        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(
                e.getClass(),
                ResponseStatus.class
        );
        if (responseStatus == null) {
            return null;
        }

        return new HttpExceptionResponse(responseStatus.code(), HttpHeaders.EMPTY);
    }

    private ApiError toApiError(HttpStatusCode statusCode, String path, String traceId) {
        return toApiError(statusCode, path, traceId, List.of());
    }

    private ApiError toApiError(
            HttpStatusCode statusCode,
            String path,
            String traceId,
            List<ApiErrorDetail> details
    ) {
        ErrorDescriptor errorDescriptor = resolveErrorDescriptor(statusCode);
        return ApiError.of(
                statusCode,
                errorDescriptor.code(),
                errorDescriptor.message(),
                path,
                traceId,
                details
        );
    }

    private ErrorDescriptor resolveErrorDescriptor(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> new ErrorDescriptor(
                    CommonErrorCode.INVALID_INPUT.getCode(),
                    CommonErrorCode.INVALID_INPUT.getMessage()
            );
            case 401 -> new ErrorDescriptor(
                    CommonErrorCode.UNAUTHORIZED.getCode(),
                    CommonErrorCode.UNAUTHORIZED.getMessage()
            );
            case 403 -> new ErrorDescriptor(
                    CommonErrorCode.FORBIDDEN.getCode(),
                    CommonErrorCode.FORBIDDEN.getMessage()
            );
            case 404 -> new ErrorDescriptor(
                    CommonErrorCode.NOT_FOUND.getCode(),
                    CommonErrorCode.NOT_FOUND.getMessage()
            );
            case 500 -> new ErrorDescriptor(
                    "CMN500001",
                    "내부 서버 오류가 발생했습니다"
            );
            default -> new ErrorDescriptor(
                    "HTTP" + statusCode.value(),
                    resolveReasonPhrase(statusCode)
            );
        };
    }

    private void logApiError(Exception e, ApiError apiError) {
        logApiError(e, apiError, Map.of());
    }

    private void logApiError(Exception e, ApiError apiError, Map<String, Object> detail) {
        withTraceId(apiError.traceId(), () -> {
            if (apiError.status() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.error(
                        "API internal error: status={}, code={}, path={}, traceId={}, publicMessage={}, publicDetails={}, exceptionType={}, internalMessage={}, detail={}",
                        apiError.status(),
                        apiError.code(),
                        apiError.path(),
                        apiError.traceId(),
                        apiError.message(),
                        apiError.details(),
                        e.getClass().getName(),
                        e.getMessage(),
                        detail,
                        e
                );
                return;
            }

            log.warn(
                    "API public error response: status={}, code={}, path={}, traceId={}, publicMessage={}, publicDetails={}, exceptionType={}, internalMessage={}, detail={}",
                    apiError.status(),
                    apiError.code(),
                    apiError.path(),
                    apiError.traceId(),
                    apiError.message(),
                    apiError.details(),
                    e.getClass().getName(),
                    e.getMessage(),
                    detail
            );
            log.debug(
                    "API internal error detail: status={}, code={}, path={}, traceId={}",
                    apiError.status(),
                    apiError.code(),
                    apiError.path(),
                    apiError.traceId(),
                    e
            );
        });
    }

    private List<ApiErrorDetail> validationDetails(Exception e) {
        if (!(e instanceof BindException bindException)) {
            return List.of();
        }

        return bindException.getAllErrors()
                .stream()
                .map(this::toApiErrorDetail)
                .toList();
    }

    private ApiErrorDetail toApiErrorDetail(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return new ApiErrorDetail(
                    fieldError.getField(),
                    resolveValidationReason(fieldError)
            );
        }

        return new ApiErrorDetail(
                error.getObjectName(),
                resolveValidationReason(error)
        );
    }

    private String resolveValidationReason(ObjectError error) {
        if (error.getDefaultMessage() != null && !error.getDefaultMessage().isBlank()) {
            return error.getDefaultMessage();
        }

        return "Invalid value";
    }

    private void withTraceId(String traceId, Runnable logging) {
        String previousTraceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            MDC.put("traceId", traceId);
        }

        try {
            logging.run();
        } finally {
            if (previousTraceId == null) {
                MDC.remove("traceId");
                return;
            }
            MDC.put("traceId", previousTraceId);
        }
    }

    private String currentTraceId() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            return spanContext.getTraceId();
        }
        return MDC.get("traceId");
    }

    private String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }

    private String resolveReasonPhrase(HttpStatusCode statusCode) {
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        if (httpStatus == null) {
            return "HTTP " + statusCode.value();
        }

        return httpStatus.getReasonPhrase();
    }

    private record ErrorDescriptor(
            String code,
            String message
    ) {
    }

    private record HttpExceptionResponse(
            HttpStatusCode statusCode,
            HttpHeaders headers
    ) {
    }
}
