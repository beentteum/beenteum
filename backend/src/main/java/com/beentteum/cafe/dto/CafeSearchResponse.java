package com.beentteum.cafe.dto;

import java.time.Instant;
import java.util.List;

import tools.jackson.databind.JsonNode;

public record CafeSearchResponse(List<Item> items, int page, int size, boolean hasNext) {

    public record Item(
            String cafeId,
            String name,
            String address,
            Location location,
            Long distanceMeters,
            Boolean wifiAvailable,
            Boolean outletAvailable,
            String noiseLevel,
            JsonNode businessHours,
            Instant spaceInfoUpdatedAt
    ) {}

    public record Location(double lat, double lng) {}
}
