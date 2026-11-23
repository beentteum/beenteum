package com.beentteum.cafe.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import com.beentteum.cafe.dto.CafeSearchQuery;
import com.beentteum.cafe.dto.CafeSearchResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class CafeSearchRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    public CafeSearchRepository(NamedParameterJdbcTemplate jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    public List<CafeSearchResponse.Item> findActiveCafes(CafeSearchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String distance = "NULL::double precision";
        if (query.originLat() != null) {
            distance = """
                    ST_Distance(c.location::geography,
                        ST_SetSRID(ST_MakePoint(:originLng, :originLat), 4326)::geography)
                    """;
            params.addValue("originLng", query.originLng()).addValue("originLat", query.originLat());
        }

        StringBuilder sql = new StringBuilder("""
                SELECT c.cafe_id, c.name, COALESCE(NULLIF(BTRIM(c.road_address), ''), c.address) AS address,
                       ST_Y(c.location) AS lat, ST_X(c.location) AS lng,
                       s.wifi_available, s.outlet_available, s.noise_level, s.business_hours,
                       s.updated_at, %s AS distance_meters
                FROM cafes c
                LEFT JOIN cafe_space_info s ON s.cafe_id = c.cafe_id
                WHERE c.status = 'ACTIVE'
                """.formatted(distance));

        if (!query.q().isEmpty()) {
            sql.append("""
                    AND (c.name ILIKE :keyword ESCAPE '!'
                      OR c.address ILIKE :keyword ESCAPE '!'
                      OR c.road_address ILIKE :keyword ESCAPE '!')
                    """);
            String keyword = query.q().replace("!", "!!").replace("%", "!%").replace("_", "!_");
            params.addValue("keyword", "%" + keyword + "%");
        }
        if (query.wifi() != null) {
            sql.append(" AND s.wifi_available = :wifi");
            params.addValue("wifi", query.wifi());
        }
        if (query.outlet() != null) {
            sql.append(" AND s.outlet_available = :outlet");
            params.addValue("outlet", query.outlet());
        }
        if (query.south() != null) {
            sql.append(" AND ST_Intersects(c.location, ST_MakeEnvelope(:west, :south, :east, :north, 4326))");
            params.addValue("west", query.west()).addValue("south", query.south())
                    .addValue("east", query.east()).addValue("north", query.north());
        }

        sql.append(switch (query.sort()) {
            case NAME_ASC -> " ORDER BY c.name ASC, c.cafe_id ASC";
            case DISTANCE_ASC -> " ORDER BY distance_meters ASC NULLS LAST, c.cafe_id ASC";
        });
        sql.append(" LIMIT :limit OFFSET :offset");
        params.addValue("limit", query.size() + 1)
                .addValue("offset", (long) query.page() * query.size());

        return jdbc.query(sql.toString(), params, this::mapItem);
    }

    private CafeSearchResponse.Item mapItem(ResultSet row, int rowNumber) throws SQLException {
        Double latitude = row.getObject("lat", Double.class);
        Double distance = row.getObject("distance_meters", Double.class);
        String businessHours = row.getString("business_hours");
        OffsetDateTime updatedAt = row.getObject("updated_at", OffsetDateTime.class);
        return new CafeSearchResponse.Item(
                Long.toString(row.getLong("cafe_id")),
                row.getString("name"),
                row.getString("address"),
                latitude == null ? null : new CafeSearchResponse.Location(latitude, row.getDouble("lng")),
                distance == null ? null : Math.round(distance),
                row.getObject("wifi_available", Boolean.class),
                row.getObject("outlet_available", Boolean.class),
                row.getString("noise_level"),
                businessHours == null ? null : jsonMapper.readTree(businessHours),
                updatedAt == null ? null : updatedAt.toInstant());
    }
}
