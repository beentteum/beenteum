
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

    // 404: 존재하지 않는 리소스
    @Test
    void 존재하지_않는_리소스는_404_ProblemDetail을_반환한다()
            throws Exception {

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value(
                        "요청한 리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.code").value(
                        "RESOURCE_NOT_FOUND"));
    }

    // 400: 입력값 검증 실패
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
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value(
                        "잘못된 입력값입니다."))
                .andExpect(jsonPath("$.code").value(
                        "INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value(
                        "이름은 필수입니다."));
    }

    // 400: 잘못된 JSON 요청
    @Test
    void 잘못된_JSON_요청은_400_ProblemDetail을_반환한다()
            throws Exception {

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value(
                        "요청 본문을 읽을 수 없습니다."))
                .andExpect(jsonPath("$.code").value(
                        "INVALID_REQUEST_BODY"));
    }

    // 405: 지원하지 않는 HTTP 메서드
    @Test
    void 지원하지_않는_HTTP_메서드는_405를_반환한다()
            throws Exception {

        mockMvc.perform(put("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value(
                        "METHOD_NOT_ALLOWED"))
                .andExpect(header().string("Allow",
                        org.hamcrest.Matchers.containsString("POST")));
    }

    // 500: 예상하지 못한 서버 오류
    @Test
    void 예상하지_못한_예외는_500_ProblemDetail을_반환한다()
            throws Exception {

        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value(
                        "Internal Server Error"))
                .andExpect(jsonPath("$.detail").value(
                        "서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.code").value(
                        "INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "내부 오류 상세 정보"))));
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

        @GetMapping("/test/server-error")
        public String serverError() {
            throw new IllegalStateException("내부 오류 상세 정보");
        }
    }

    // 입력값 검증 테스트용 DTO
    record TestRequest(
            @NotBlank(message = "이름은 필수입니다.")
            String name
    ) {
    }
}
