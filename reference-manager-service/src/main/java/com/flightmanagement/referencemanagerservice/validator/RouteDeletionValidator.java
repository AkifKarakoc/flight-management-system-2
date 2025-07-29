package com.flightmanagement.referencemanagerservice.validator;

import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RouteDeletionValidator {

    public void validateDeletion(Long routeId) throws BusinessException {
        DeletionCheckResult result = checkDependencies(routeId);

        if (!result.isCanDelete()) {
            throw new BusinessException(
                    String.format("Cannot delete route: %s", result.getReason())
            );
        }
    }

    public DeletionCheckResult checkDependencies(Long routeId) {
        List<String> blockers = new ArrayList<>();
        Map<String, Integer> dependentEntities = new HashMap<>();

        // Future: Flight kontrolü
        // long flightCount = flightRepository.countByRouteId(routeId);
        // if (flightCount > 0) {
        //     blockers.add(String.format("%d active flight(s)", flightCount));
        //     dependentEntities.put("flights", (int) flightCount);
        // }

        return DeletionCheckResult.builder()
                .canDelete(blockers.isEmpty())
                .reason(blockers.isEmpty() ? "No dependencies found" : String.join(", ", blockers))
                .dependentEntities(dependentEntities)
                .build();
    }
}