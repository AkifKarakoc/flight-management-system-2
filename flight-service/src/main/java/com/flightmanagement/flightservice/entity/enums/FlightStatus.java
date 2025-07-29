package com.flightmanagement.flightservice.entity.enums;

public enum FlightStatus {
    SCHEDULED,      // Planlandı
    BOARDING,       // Yolcu alımı başladı
    DEPARTED,       // Kalktı
    ARRIVED,        // Geldi
    CANCELLED,      // İptal edildi
    DELAYED,        // Gecikti
    DIVERTED,       // Başka havaalanına yönlendirildi
    RETURNING       // Geri dönüyor
}