package com.beentteum.cafe.service;

import java.util.List;

import com.beentteum.cafe.dto.CafeSearchQuery;
import com.beentteum.cafe.dto.CafeSearchResponse;
import com.beentteum.cafe.repository.CafeSearchRepository;
import com.beentteum.common.exception.BusinessException;
import com.beentteum.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class CafeSearchService {

    private final CafeSearchRepository repository;

    public CafeSearchService(CafeSearchRepository repository) {
        this.repository = repository;
    }

    public CafeSearchResponse search(CafeSearchQuery query) {
        if (query.q().indexOf(0) >= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        validateCoordinates(query);
        List<CafeSearchResponse.Item> rows = repository.findActiveCafes(query);
        boolean hasNext = rows.size() > query.size();
        return new CafeSearchResponse(
                List.copyOf(rows.subList(0, Math.min(rows.size(), query.size()))),
                query.page(), query.size(), hasNext);
    }

    private void validateCoordinates(CafeSearchQuery query) {
        boolean hasBounds = query.south() != null || query.west() != null
                || query.north() != null || query.east() != null;
        if (hasBounds && (!inRange(query.south(), 90) || !inRange(query.north(), 90)
                || !inRange(query.west(), 180) || !inRange(query.east(), 180)
                || query.south() >= query.north() || query.west() >= query.east())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        boolean needsOrigin = query.originLat() != null || query.originLng() != null
                || query.sort() == CafeSearchQuery.Sort.DISTANCE_ASC;
        if (needsOrigin && (!inRange(query.originLat(), 90) || !inRange(query.originLng(), 180))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private boolean inRange(Double value, int maximum) {
        return value != null && value >= -maximum && value <= maximum;
    }
}
