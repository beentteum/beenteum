
package com.beentteum.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = createMockMvc();

    private static MockMvc createMockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // 404: 비즈니스 예외
    @Test
    void 존재하지_않는_리소스는_404_ProblemDetail을_반환한다()
            throws Exception {

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(
                        "RESOURCE_NOT_FOUND"));
    }

    // 400: DTO 검증 실패
    @Test
    void 필수_입력값이_비어있으면_400_검증_오류를_반환한다()
            throws Exception {

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(
                        "INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].reason").value(
        "이름은 필수입니다."));
    }

    // 400: 잘못된 JSON
    @Test
    void 잘못된_JSON_요청은_400을_반환한다()
            throws Exception {

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(
                        "INVALID_REQUEST_BODY"));
    }

    // 400: 필수 요청 파라미터 누락
    @Test
    void 필수_요청_파라미터가_없으면_400을_반환한다()
            throws Exception {

        mockMvc.perform(get("/test/required-param"))
                .andExpect(status().isBadRequest());
    }

    // 405: 지원하지 않는 HTTP 메서드
    @Test
    void 지원하지_않는_HTTP_메서드는_405를_반환한다()
            throws Exception {

        mockMvc.perform(put("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(
                        "Allow",
                        org.hamcrest.Matchers.containsString("POST")));
    }

    // 415: 지원하지 않는 Content-Type
    @Test
    void 지원하지_않는_미디어_타입은_415를_반환한다()
            throws Exception {

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("테스트"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // 500: 예상하지 못한 서버 오류
    @Test
    void 예상하지_못한_예외는_500을_반환한다()
            throws Exception {

        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value(
                        "INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.detail").value(
                        "서버 내부 오류가 발생했습니다."));
    }

    // 테스트 전용 컨트롤러
    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        public String notFound() {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND);
        }

        @PostMapping("/test/validation")
        public String validation(
                @Valid @RequestBody TestRequest request) {
            return request.name();
        }

        @GetMapping("/test/required-param")
        public String requiredParam(
                @RequestParam String name) {
            return name;
        }

        @GetMapping("/test/server-error")
        public String serverError() {
            throw new IllegalStateException("내부 오류 상세 정보");
        }
    }

    record TestRequest(
            @NotBlank(message = "이름은 필수입니다.")
            String name
    ) {
    }
}
