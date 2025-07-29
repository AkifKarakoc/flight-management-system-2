package com.flightmanagement.flightservice.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class CsvUploadResult {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<String> errors;
    private String message;

    public String getSummary() {
        return String.format("Processed %d rows: %d successful, %d failed",
                totalRows, successCount, failureCount);
    }

    public boolean isPartialSuccess() {
        return successCount > 0 && failureCount > 0;
    }

    public boolean isCompleteSuccess() {
        return failureCount == 0 && successCount > 0;
    }

    public boolean isCompleteFailure() {
        return successCount == 0;
    }
}