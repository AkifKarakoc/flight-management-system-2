package com.flightmanagement.referencemanagerservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionCheckResult {
    private boolean canDelete;
    private String reason;
    private Map<String, Integer> dependentEntities;
}