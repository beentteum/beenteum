
package com.beentteum.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void 메시지와_데이터를_포함한_성공_응답을_생성한다() {
        ApiResponse<String> response =
                ApiResponse.success("조회 성공", "카페 정보");

        assertEquals("조회 성공", response.message());
        assertEquals("카페 정보", response.data());
    }

    @Test
    void 데이터만_전달하면_기본_성공_메시지를_사용한다() {
        ApiResponse<Integer> response = ApiResponse.success(10);

        assertEquals("요청이 성공적으로 처리되었습니다.", response.message());
        assertEquals(10, response.data());
    }

    @Test
    void 메시지만_전달하면_데이터는_null이다() {
        ApiResponse<Void> response = ApiResponse.success("삭제 완료");

        assertEquals("삭제 완료", response.message());
        assertNull(response.data());
    }
}
