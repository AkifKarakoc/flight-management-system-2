package com.flightmanagement.referencemanagerservice.entity.enums;

public enum RouteVisibility {
    PRIVATE,    // Sadece oluşturan kullanıcı görür
    SHARED,     // Aynı havayolu çalışanları görür
    PUBLIC      // Herkes görür (admin route'ları)
}