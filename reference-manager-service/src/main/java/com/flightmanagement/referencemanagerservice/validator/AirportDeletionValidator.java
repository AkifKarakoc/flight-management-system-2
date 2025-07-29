package com.flightmanagement.referencemanagerservice.validator;

import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.exception.BusinessException;
import com.flightmanagement.referencemanagerservice.repository.CrewMemberRepository;
import com.flightmanagement.referencemanagerservice.repository.RouteRepository;
import com.flightmanagement.referencemanagerservice.repository.RouteSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AirportDeletionValidator {

    private final RouteRepository routeRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final RouteSegmentRepository routeSegmentRepository;

    public void validateDeletion(Long airportId) throws BusinessException {
        DeletionCheckResult result = checkDependencies(airportId);

        if (!result.isCanDelete()) {
            throw new BusinessException(
                    String.format("Cannot delete airport: %s", result.getReason())
            );
        }
    }

    public DeletionCheckResult checkDependencies(Long airportId) {
        List<String> blockers = new ArrayList<>();
        Map<String, Integer> dependentEntities = new HashMap<>();

        // Route kontrolü (origin veya destination olarak kullanılıyor mu?)
        long totalRoutes = routeSegmentRepository.countByAirportId(airportId);
        if (totalRoutes > 0) {
            blockers.add(String.format("%d active route(s)", totalRoutes));
            dependentEntities.put("routes", (int) totalRoutes);
        }

        return DeletionCheckResult.builder()
                .canDelete(blockers.isEmpty())
                .reason(blockers.isEmpty() ? "No dependencies found" : String.join(", ", blockers))
                .dependentEntities(dependentEntities)
                .build();
    }
}