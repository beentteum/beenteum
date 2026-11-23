package com.beentteum.cafe.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.UUID;

import com.beentteum.cafe.dto.CafeSearchQuery;
import com.beentteum.cafe.dto.CafeSearchQuery.Sort;
import com.beentteum.cafe.dto.CafeSearchResponse.Item;
import com.beentteum.cafe.service.CafeSearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import tools.jackson.databind.json.JsonMapper;

@Tag("db")
class CafeSearchPostgisTest {

    private Connection connection;
    private CafeSearchService service;

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        String url = System.getenv("CAFE_SEARCH_TEST_DB_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("CAFE_SEARCH_TEST_DB_URL must point to a dedicated PostgreSQL test database");
        }
        connection = new DriverManagerDataSource(url, System.getenv("CAFE_SEARCH_TEST_DB_USER"),
                System.getenv("CAFE_SEARCH_TEST_DB_PASSWORD")).getConnection();
        connection.setAutoCommit(false);
        String schema = "cafe_search_" + UUID.randomUUID().toString().replace("-", "");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET LOCAL search_path TO " + schema + ", public");
        }
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__init_schema.sql"));
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO cafes (cafe_id, name, address, road_address, location, status) VALUES
                      (1, 'Alpha Cafe', '구주소 동네', '새주소 도로', ST_SetSRID(ST_MakePoint(127, 37), 4326), 'ACTIVE'),
                      (2, 'Beta Cafe', '다른 동네', NULL, ST_SetSRID(ST_MakePoint(127.05, 37), 4326), 'ACTIVE'),
                      (3, 'Cafe 100%', NULL, NULL, NULL, 'ACTIVE'),
                      (4, 'Closed Cafe', NULL, NULL, ST_SetSRID(ST_MakePoint(127, 37), 4326), 'INACTIVE');
                    INSERT INTO cafe_space_info (cafe_id, wifi_available, outlet_available, business_hours) VALUES
                      (1, TRUE, FALSE, '{"mon":"09:00-18:00"}'), (2, TRUE, TRUE, NULL);
                    """);
        }
        var jdbc = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true));
        service = new CafeSearchService(new CafeSearchRepository(jdbc, JsonMapper.builder().build()));
    }

    @AfterEach
    void rollbackSchemaAndFixtures() throws Exception {
        if (connection != null) {
            try (Connection testConnection = connection) {
                testConnection.rollback();
            }
        }
    }

    @Test
    void searchesNameAndBothAddressesAndEscapesWildcards() {
        for (String keyword : new String[]{"alpha", "구주소", "새주소"}) {
            var result = service.search(query(keyword, null, null, 0, 20));
            assertThat(result.items()).extracting(Item::cafeId).containsExactly("1");
            assertThat(result.items().getFirst().address()).isEqualTo("새주소 도로");
        }
        assertThat(service.search(query("%", null, null, 0, 20)).items())
                .extracting(Item::cafeId).containsExactly("3");
        assertThat(service.search(query("Closed", null, null, 0, 20)).items()).isEmpty();
    }

    @Test
    void combinesBooleanFiltersAndPreservesUnknownSpaceInformation() {
        assertThat(service.search(query(null, true, true, 0, 20)).items())
                .extracting(Item::cafeId).containsExactly("2");
        assertThat(service.search(query(null, true, false, 0, 20)).items())
                .extracting(Item::cafeId).containsExactly("1");
        Item item = service.search(query("100%", null, null, 0, 20)).items().getFirst();
        assertThat(item.wifiAvailable()).isNull();
        assertThat(item.outletAvailable()).isNull();
        assertThat(item.businessHours()).isNull();
        assertThat(item.spaceInfoUpdatedAt()).isNull();
        assertThat(item.location()).isNull();
    }

    @Test
    void boundsIncludeBoundaryAndExcludeMissingLocations() {
        var query = new CafeSearchQuery(null, null, null, 37.0, 127.0, 37.01, 127.01,
                null, null, null, null, null);
        assertThat(service.search(query).items()).extracting(Item::cafeId).containsExactly("1");
    }

    @Test
    void sortsByDistanceWithMissingCoordinatesLast() {
        var query = new CafeSearchQuery(null, null, null, null, null, null, null,
                37.0, 127.05, Sort.DISTANCE_ASC, null, null);
        var items = service.search(query).items();
        assertThat(items).extracting(Item::cafeId).containsExactly("2", "1", "3");
        assertThat(items.getFirst().distanceMeters()).isZero();
        assertThat(items.get(1).distanceMeters()).isBetween(4400L, 4500L);
        assertThat(items.getLast().distanceMeters()).isNull();
    }

    @Test
    void pagesByNameWithLookaheadAndReturnsEmptyBeyondLastPage() {
        var first = service.search(query(null, null, null, 0, 2));
        var last = service.search(query(null, null, null, 1, 2));
        assertThat(first.items()).extracting(Item::cafeId).containsExactly("1", "2");
        assertThat(first.hasNext()).isTrue();
        assertThat(last.items()).extracting(Item::cafeId).containsExactly("3");
        assertThat(last.hasNext()).isFalse();
        assertThat(service.search(query(null, null, null, 2, 2)).items()).isEmpty();
    }

    private CafeSearchQuery query(String keyword, Boolean wifi, Boolean outlet, int page, int size) {
        return new CafeSearchQuery(keyword, wifi, outlet, null, null, null, null,
                null, null, null, page, size);
    }
}
