package com.beentteum.cafe.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CafeSearchQuery(
        @Size(max = 200) String q,
        Boolean wifi,
        Boolean outlet,
        Double south,
        Double west,
        Double north,
        Double east,
        Double originLat,
        Double originLng,
        Sort sort,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public CafeSearchQuery {
        q = q == null ? "" : q.strip();
        sort = sort == null ? Sort.NAME_ASC : sort;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    public enum Sort {
        NAME_ASC,
        DISTANCE_ASC
    }
}
