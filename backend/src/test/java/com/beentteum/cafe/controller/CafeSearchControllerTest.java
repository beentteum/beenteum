package com.beentteum.cafe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.beentteum.cafe.dto.CafeSearchResponse;
import com.beentteum.cafe.repository.CafeSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CafeSearchControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean CafeSearchRepository repository;

    @Test
    void publicGetUsesCommonResponseAndSerializesIdAsString() throws Exception {
        when(repository.findActiveCafes(any())).thenReturn(List.of(new CafeSearchResponse.Item(
                "9007199254740993", "Cafe", null, null, null, null, null, null, null, null)));

        mvc.perform(get("/api/cafes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data.items[0].cafeId").isString())
                .andExpect(jsonPath("$.data.items[0].cafeId").value("9007199254740993"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void keywordWithNullCharacterIsRejectedBeforeDatabaseAccess() throws Exception {
        mvc.perform(get("/api/cafes").param("q", "카페\0검색"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        verifyNoInteractions(repository);
    }

    @Test
    void otherMethodsAndCafePathsStillRequireAuthentication() throws Exception {
        mvc.perform(post("/api/cafes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cafes/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBindingAndInvalidCoordinatePairsUseTheCommonErrorCode() throws Exception {
        mvc.perform(get("/api/cafes").param("page", "invalid").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        mvc.perform(get("/api/cafes").param("sort", "DISTANCE_ASC").param("originLat", "37"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
