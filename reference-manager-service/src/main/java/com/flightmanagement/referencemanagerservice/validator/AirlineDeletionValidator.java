package com.flightmanagement.referencemanagerservice.validator;

import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.exception.BusinessException;
import com.flightmanagement.referencemanagerservice.repository.AircraftRepository;
import com.flightmanagement.referencemanagerservice.repository.CrewMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AirlineDeletionValidator {

    private final AircraftRepository aircraftRepository;
    private final CrewMemberRepository crewMemberRepository;

    public void validateDeletion(Long airlineId) throws BusinessException {
        DeletionCheckResult result = checkDependencies(airlineId);

        if (!result.isCanDelete()) {
            throw new BusinessException(
                    String.format("Cannot delete airline: %s", result.getReason())
            );
        }
    }

    public DeletionCheckResult checkDependencies(Long airlineId) {
        List<String> blockers = new ArrayList<>();
        Map<String, Integer> dependentEntities = new HashMap<>();

        // Aircraft count
        long aircraftCount = aircraftRepository.countByAirlineId(airlineId);
        if (aircraftCount > 0) {
            blockers.add(String.format("%d active aircraft(s)", aircraftCount));
            dependentEntities.put("aircrafts", (int) aircraftCount);
        }

        // Crew member count
        long crewCount = crewMemberRepository.countByAirlineId(airlineId);
        if (crewCount > 0) {
            blockers.add(String.format("%d active crew member(s)", crewCount));
            dependentEntities.put("crewMembers", (int) crewCount);
        }

        return DeletionCheckResult.builder()
                .canDelete(blockers.isEmpty())
                .reason(blockers.isEmpty() ? "No dependencies found" : String.join(", ", blockers))
                .dependentEntities(dependentEntities)
                .build();
    }
}