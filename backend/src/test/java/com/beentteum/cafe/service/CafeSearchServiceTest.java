package com.beentteum.cafe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import com.beentteum.cafe.dto.CafeSearchQuery;
import com.beentteum.cafe.dto.CafeSearchResponse.Item;
import com.beentteum.cafe.repository.CafeSearchRepository;
import com.beentteum.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class CafeSearchServiceTest {

    private final CafeSearchRepository repository = mock(CafeSearchRepository.class);
    private final CafeSearchService service = new CafeSearchService(repository);

    @Test
    void trimsLookaheadAndPreservesPageMetadata() {
        CafeSearchQuery query = query(null);
        Item item = new Item("1", "Cafe", null, null, null, null, null, null, null, null);
        when(repository.findActiveCafes(query)).thenReturn(List.of(item, item));

        var result = service.search(query);
        assertThat(result.items()).containsExactly(item);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    void emptyPageDoesNotHaveNextPage() {
        CafeSearchQuery query = query(null);
        when(repository.findActiveCafes(query)).thenReturn(List.of());

        var result = service.search(query);
        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void incompleteBoundsAreRejectedBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service.search(query(37.0))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository);
    }

    private CafeSearchQuery query(Double south) {
        return new CafeSearchQuery(null, null, null, south, null, null, null, null, null, null, 2, 1);
    }
}
