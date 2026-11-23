package com.beentteum.cafe.controller;

import com.beentteum.cafe.dto.CafeSearchQuery;
import com.beentteum.cafe.dto.CafeSearchResponse;
import com.beentteum.cafe.service.CafeSearchService;
import com.beentteum.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CafeSearchController {

    private final CafeSearchService service;

    public CafeSearchController(CafeSearchService service) {
        this.service = service;
    }

    @GetMapping("/api/cafes")
    public ApiResponse<CafeSearchResponse> search(@Valid @ModelAttribute CafeSearchQuery query) {
        return ApiResponse.success(service.search(query));
    }
}
