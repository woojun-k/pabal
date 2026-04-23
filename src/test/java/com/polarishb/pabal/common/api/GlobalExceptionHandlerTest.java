package com.polarishb.pabal.common.api;

import com.polarishb.pabal.common.exception.GlobalException;
import com.polarishb.pabal.common.exception.code.CommonErrorCode;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController(validator))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void handleMethodArgumentNotValid_returns_bad_request_with_details() throws Exception {
        try (Scope ignored = otelSpan().makeCurrent()) {
            mockMvc.perform(
                            post("/validated")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "name": ""
                                            }
                                            """)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("CMN002"))
                    .andExpect(jsonPath("$.message").value("잘못된 입력입니다"))
                    .andExpect(jsonPath("$.path").value("/validated"))
                    .andExpect(jsonPath("$.traceId").value(TRACE_ID))
                    .andExpect(jsonPath("$.details[0].field").value("name"))
                    .andExpect(jsonPath("$.details[0].reason").value("name is required"))
                    .andExpect(jsonPath("$.payload").doesNotExist());
        }
    }

    @Test
    void handleBindException_returns_bad_request_with_details() throws Exception {
        mockMvc.perform(get("/bind").param("name", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("CMN002"))
                .andExpect(jsonPath("$.message").value("잘못된 입력입니다"))
                .andExpect(jsonPath("$.path").value("/bind"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].reason").value("name is required"));
    }

    @Test
    void handleHandlerMethodValidationException_returns_bad_request_with_details() throws Exception {
        mockMvc.perform(get("/method-validation").param("name", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("CMN002"))
                .andExpect(jsonPath("$.message").value("잘못된 입력입니다"))
                .andExpect(jsonPath("$.path").value("/method-validation"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].reason").value("name is required"));
    }

    @Test
    void handleConstraintViolationException_returns_bad_request_with_details() throws Exception {
        mockMvc.perform(get("/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("CMN002"))
                .andExpect(jsonPath("$.message").value("잘못된 입력입니다"))
                .andExpect(jsonPath("$.path").value("/constraint"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].reason").value("name is required"));
    }

    @Test
    void handleAccessDeniedException_returns_forbidden() throws Exception {
        mockMvc.perform(get("/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("CMN004"))
                .andExpect(jsonPath("$.path").value("/denied"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void handleObjectOptimisticLockingFailureException_returns_conflict() throws Exception {
        mockMvc.perform(get("/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("HTTP409"))
                .andExpect(jsonPath("$.path").value("/optimistic-lock"));
    }

    @Test
    void handleDataIntegrityViolationException_returns_conflict() throws Exception {
        mockMvc.perform(get("/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("HTTP409"))
                .andExpect(jsonPath("$.path").value("/data-integrity"));
    }

    @Test
    void handleException_preserves_binding_bad_request_status() throws Exception {
        mockMvc.perform(get("/typed/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("CMN002"))
                .andExpect(jsonPath("$.path").value("/typed/not-a-uuid"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void handleException_preserves_response_status_exception_status() throws Exception {
        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CMN005"))
                .andExpect(jsonPath("$.path").value("/missing"));
    }

    @Test
    void handleException_preserves_response_status_annotation_status() throws Exception {
        mockMvc.perform(get("/annotated-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("HTTP409"))
                .andExpect(jsonPath("$.path").value("/annotated-conflict"));
    }

    @Test
    void handleGlobalException_returns_public_api_error_without_payload() throws Exception {
        mockMvc.perform(get("/global"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("CMN002"))
                .andExpect(jsonPath("$.message").value("잘못된 입력입니다"))
                .andExpect(jsonPath("$.path").value("/global"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.payload").doesNotExist());
    }

    @Test
    void handleException_returns_internal_server_error_for_unexpected_exception() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("CMN500001"))
                .andExpect(jsonPath("$.message").value("내부 서버 오류가 발생했습니다"))
                .andExpect(jsonPath("$.path").value("/unexpected"));
    }

    @RestController
    private static class TestController {

        private final Validator validator;

        private TestController(Validator validator) {
            this.validator = validator;
        }

        @PostMapping("/validated")
        void validated(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/bind")
        void bind(@Valid @ModelAttribute TestForm form) {
        }

        @GetMapping("/method-validation")
        void methodValidation(@RequestParam @NotBlank(message = "name is required") String name) {
        }

        @GetMapping("/constraint")
        void constraint() {
            TestRequest request = new TestRequest("");
            Set<ConstraintViolation<TestRequest>> violations = validator.validate(request);
            throw new ConstraintViolationException(violations);
        }

        @GetMapping("/denied")
        void denied() {
            throw new AccessDeniedException("forbidden");
        }

        @GetMapping("/optimistic-lock")
        void optimisticLock() {
            throw new ObjectOptimisticLockingFailureException(TestEntity.class, 1L);
        }

        @GetMapping("/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException("duplicate key");
        }

        @GetMapping("/missing")
        void missing() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "missing");
        }

        @GetMapping("/typed/{id}")
        void typed(@PathVariable UUID id) {
        }

        @GetMapping("/annotated-conflict")
        void annotatedConflict() {
            throw new ConflictException();
        }

        @GetMapping("/global")
        void global() {
            throw new TestGlobalException();
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("boom");
        }
    }

    private record TestRequest(
            @NotBlank(message = "name is required") String name
    ) {
    }

    private static class TestForm {
        @NotBlank(message = "name is required")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static class TestEntity {
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    private static class ConflictException extends RuntimeException {
    }

    private static class TestGlobalException extends GlobalException {
        private TestGlobalException() {
            super(CommonErrorCode.INVALID_INPUT, "internal validation detail", Map.of("field", "secret"));
        }
    }

    private Span otelSpan() {
        return Span.wrap(SpanContext.create(
                TRACE_ID,
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()
        ));
    }
}