package com.flightmanagement.flightarchiveservice.service;

import com.flightmanagement.flightarchiveservice.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendArchiveUpdate(String flightNumber, Object archiveData, Long archiveId) {
        WebSocketMessage message = WebSocketMessage.create("ARCHIVED", "FLIGHT_ARCHIVE", archiveData, archiveId);
        message.setFlightNumber(flightNumber);

        sendMessage("/topic/archive", message);
        sendMessage("/topic/archive/flights", message);
        sendMessage("/topic/updates", message);
        log.debug("Sent archive message for flight: {}", flightNumber);
    }

    public void sendKpiUpdate(String kpiType, Object kpiData) {
        WebSocketMessage message = WebSocketMessage.create("KPI_UPDATE", "KPI", kpiData, null);
        message.setEventType(kpiType);

        sendMessage("/topic/archive/kpi", message);
        sendMessage("/topic/kpi/" + kpiType, message);
        sendMessage("/topic/updates", message);
        log.debug("Sent KPI update for type: {}", kpiType);
    }

    public void sendReportGenerated(String reportType, Object reportData, Long reportId) {
        WebSocketMessage message = WebSocketMessage.create("REPORT_GENERATED", "REPORT", reportData, reportId);
        message.setEventType(reportType);

        sendMessage("/topic/archive/reports", message);
        sendMessage("/topic/reports/" + reportType, message);
        sendMessage("/topic/updates", message);
        log.debug("Sent report generated message for type: {}", reportType);
    }

    public void sendBatchProcessUpdate(String batchType, Object batchData) {
        WebSocketMessage message = WebSocketMessage.create("BATCH_PROCESSED", "BATCH", batchData, null);
        message.setEventType(batchType);

        sendMessage("/topic/archive/batch", message);
        sendMessage("/topic/updates", message);
        log.info("Sent batch process update for type: {}", batchType);
    }

    private void sendMessage(String destination, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message to {}: {}", destination, e.getMessage());
        }
    }
}