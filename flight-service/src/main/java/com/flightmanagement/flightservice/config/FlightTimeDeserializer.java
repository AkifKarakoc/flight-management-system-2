package com.flightmanagement.flightservice.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class FlightTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    };

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        text = text.trim();
        log.debug("Deserializing flight time: '{}'", text);

        // Sadece saat formatı (HH:mm veya H:mm) kontrolü
        if (text.matches("^\\d{1,2}:\\d{2}$")) {
            try {
                LocalTime time = LocalTime.parse(text);
                LocalDate baseDate = LocalDate.now();

                // Context'ten flight date almaya çalış (eğer varsa)
                try {
                    Object currentValue = context.getParser().getCurrentValue();
                    if (currentValue != null) {
                        // Reflection ile flightDate field'ını bul
                        java.lang.reflect.Field dateField = currentValue.getClass().getDeclaredField("flightDate");
                        dateField.setAccessible(true);
                        LocalDate flightDate = (LocalDate) dateField.get(currentValue);
                        if (flightDate != null) {
                            baseDate = flightDate;
                            log.debug("Using flight date {} for time parsing", flightDate);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not extract flight date, using current date");
                }

                LocalDateTime result = baseDate.atTime(time);
                log.debug("Parsed time '{}' as {}", text, result);
                return result;
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse time format: {}", text);
                throw new IOException("Invalid time format: " + text);
            }
        }

        // Tam datetime formatları için parsing
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                LocalDateTime result = LocalDateTime.parse(text, formatter);
                log.debug("Parsed datetime '{}' as {}", text, result);
                return result;
            } catch (DateTimeParseException ignored) {
                // Bir sonraki formatter'ı dene
            }
        }

        log.error("Unable to parse datetime string: '{}'", text);
        throw new IOException("Unable to parse datetime: " + text + ". Supported formats: yyyy-MM-dd HH:mm, HH:mm");
    }
}