# Reference Manager Service - Developer Documentation

## 📋 Table of Contents
1. [Service Overview](#service-overview)
2. [Architecture & Tech Stack](#architecture--tech-stack)
3. [Database Schema](#database-schema)
4. [API Endpoints](#api-endpoints)
5. [Authentication & Authorization](#authentication--authorization)
6. [Real-time Features](#real-time-features)
7. [Integration Guide](#integration-guide)
8. [Data Models](#data-models)
9. [Error Handling](#error-handling)
10. [Performance Considerations](#performance-considerations)
11. [Deployment Guide](#deployment-guide)

---

## 🏗️ Service Overview

### Purpose
Reference Manager Service manages all reference data for the Flight Management System including airlines, airports, aircrafts, crew members, and routes. It serves as the central authority for master data used across all microservices.

### Key Features
- ✅ **Complete CRUD Operations** for all reference entities
- ✅ **JWT Authentication & Role-based Authorization**
- ✅ **Real-time Updates** via WebSocket
- ✅ **Event Publishing** via Kafka
- ✅ **Multi-segment Route Support**
- ✅ **Dependency Checking** before deletions
- ✅ **Database Versioning** with Liquibase
- ✅ **Data Validation** & Error Handling
- ✅ **Pagination Support**
- ✅ **Advanced Route Management** with ownership

### Service Details
- **Port**: 8081
- **Base URL**: `http://localhost:8081`
- **Database**: MySQL (Port 3308)
- **Status**: Production Ready ✅

---

## 🛠️ Architecture & Tech Stack

### Core Technologies
- **Framework**: Spring Boot 3.5.3
- **Language**: Java 17
- **Database**: MySQL 8.0
- **ORM**: JPA/Hibernate
- **Security**: Spring Security + JWT
- **Message Broker**: Apache Kafka
- **Real-time**: WebSocket (STOMP)
- **Migration**: Liquibase
- **Mapping**: MapStruct
- **Build Tool**: Maven

### Design Patterns
- **Repository Pattern**: Data access layer
- **Service Layer Pattern**: Business logic separation
- **DTO Pattern**: Data transfer objects
- **Event-Driven Architecture**: Kafka events
- **Dependency Injection**: Spring IoC

### Project Structure
```
src/main/java/com/flightmanagement/referencemanagerservice/
├── config/                 # Configuration classes
├── controller/             # REST endpoints
├── dto/                   # Data transfer objects
├── entity/                # JPA entities
├── exception/             # Exception handling
├── mapper/                # MapStruct mappers
├── repository/            # Data access layer
├── security/              # JWT & Security
├── service/               # Business logic
└── validator/             # Business validation
```

---

## 🗄️ Database Schema

### Core Tables

#### Airlines
```sql
CREATE TABLE airlines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    iata_code VARCHAR(3) UNIQUE NOT NULL,
    icao_code VARCHAR(4) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(100),
    type VARCHAR(50), -- FULL_SERVICE, LOW_COST, CARGO, CHARTER
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### Airports
```sql
CREATE TABLE airports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    iata_code VARCHAR(3) UNIQUE NOT NULL,
    icao_code VARCHAR(4) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    timezone VARCHAR(50),
    latitude DOUBLE,
    longitude DOUBLE,
    elevation INT,
    type VARCHAR(50), -- INTERNATIONAL, DOMESTIC, CARGO, MILITARY
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### Routes (Multi-segment Support)
```sql
CREATE TABLE routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_code VARCHAR(50) UNIQUE NOT NULL,
    route_name VARCHAR(200) NOT NULL,
    route_type VARCHAR(30) NOT NULL, -- DOMESTIC, INTERNATIONAL, CONTINENTAL
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE', -- PRIVATE, SHARED, PUBLIC
    
    -- Legacy fields (nullable for backward compatibility)
    origin_airport_id BIGINT NULL,
    destination_airport_id BIGINT NULL,
    
    -- Common fields
    distance INT,
    estimated_flight_time INT,
    active BOOLEAN DEFAULT TRUE,
    
    -- Ownership
    created_by_user_id BIGINT,
    airline_id BIGINT,
    is_multi_segment BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### Route Segments
```sql
CREATE TABLE route_segments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    segment_order INTEGER NOT NULL,
    origin_airport_id BIGINT NOT NULL,
    destination_airport_id BIGINT NOT NULL,
    distance INTEGER,
    estimated_flight_time INTEGER,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_route_segment_order (route_id, segment_order),
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    FOREIGN KEY (origin_airport_id) REFERENCES airports(id),
    FOREIGN KEY (destination_airport_id) REFERENCES airports(id)
);
```

### Key Relationships
- **Airlines** → **Aircrafts** (1:N)
- **Airlines** → **Crew Members** (1:N)
- **Airports** → **Route Segments** (1:N origin, 1:N destination)
- **Routes** → **Route Segments** (1:N)

---

## 🔌 API Endpoints

### Authentication Endpoints

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",      # or "user"
  "password": "admin123"    # or "user123"
}

Response:
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Airlines API

#### Get All Airlines (Paginated)
```http
GET /api/v1/airlines?page=0&size=10&sort=name,asc
Authorization: Bearer {token}
```

#### Create Airline (Admin Only)
```http
POST /api/v1/airlines
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "iataCode": "TK",
  "icaoCode": "THY",
  "name": "Turkish Airlines",
  "country": "Turkey",
  "type": "FULL_SERVICE",
  "active": true
}
```

#### Check Deletion Dependencies
```http
GET /api/v1/airlines/{id}/deletion-check
Authorization: Bearer {token}

Response:
{
  "canDelete": false,
  "reason": "2 active aircraft(s), 5 active crew member(s)",
  "dependentEntities": {
    "aircrafts": 2,
    "crewMembers": 5
  }
}
```

### Routes API (Advanced)

#### Create Multi-Segment Route
```http
POST /api/v1/routes
Authorization: Bearer {token}
Content-Type: application/json

{
  "routeCode": "TK-TOUR",
  "routeName": "Turkey Grand Tour Route",
  "active": true,
  "routeType": "DOMESTIC",
  "visibility": "SHARED",
  "airlineId": 1,
  "segments": [
    {
      "segmentOrder": 1,
      "originAirportId": 1,
      "destinationAirportId": 2,
      "distance": 450,
      "estimatedFlightTime": 75,
      "active": true
    },
    {
      "segmentOrder": 2,
      "originAirportId": 2,
      "destinationAirportId": 3,
      "distance": 320,
      "estimatedFlightTime": 55,
      "active": true
    }
  ]
}
```

#### Get User's Routes
```http
GET /api/v1/routes/my-routes
Authorization: Bearer {token}
```

#### Get Shared Routes for Airline
```http
GET /api/v1/routes/airline/{airlineId}/shared
Authorization: Bearer {token}
```

### Complete Endpoint List

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/login` | None | User authentication |
| GET | `/api/v1/airlines` | USER | Get paginated airlines |
| POST | `/api/v1/airlines` | ADMIN | Create airline |
| PUT | `/api/v1/airlines/{id}` | ADMIN | Update airline |
| DELETE | `/api/v1/airlines/{id}` | ADMIN | Delete airline |
| GET | `/api/v1/airlines/{id}/deletion-check` | USER | Check deletion dependencies |
| GET | `/api/v1/airports` | USER | Get paginated airports |
| POST | `/api/v1/airports` | ADMIN | Create airport |
| GET | `/api/v1/aircrafts` | USER | Get paginated aircrafts |
| GET | `/api/v1/aircrafts/airline/{id}` | USER | Get aircrafts by airline |
| GET | `/api/v1/crew-members` | USER | Get all crew members |
| GET | `/api/v1/routes` | USER | Get routes for user |
| GET | `/api/v1/routes/my-routes` | USER | Get user's own routes |
| POST | `/api/v1/routes` | USER | Create route |
| PUT | `/api/v1/routes/{id}` | USER/ADMIN | Update route (ownership check) |
| DELETE | `/api/v1/routes/{id}` | USER/ADMIN | Delete route (ownership check) |

---

## 🔐 Authentication & Authorization

### JWT Configuration
- **Secret**: Configured in application.yml
- **Expiration**: 24 hours (86400000 ms)
- **Algorithm**: HMAC SHA-256

### Test Users
```yaml
Admin User:
  username: admin
  password: admin123
  role: ROLE_ADMIN

Regular User:
  username: user
  password: user123
  role: ROLE_USER
```

### Permission Matrix

| Operation | USER | ADMIN |
|-----------|------|-------|
| View Airlines/Airports/Aircrafts | ✅ | ✅ |
| Create Airlines/Airports/Aircrafts | ❌ | ✅ |
| Update Airlines/Airports/Aircrafts | ❌ | ✅ |
| Delete Airlines/Airports/Aircrafts | ❌ | ✅ |
| View Routes | ✅ (filtered) | ✅ (all) |
| Create Routes | ✅ | ✅ |
| Update Own Routes | ✅ | ✅ |
| Update Any Routes | ❌ | ✅ |
| Delete Own Routes | ✅ | ✅ |
| Delete Any Routes | ❌ | ✅ |

### Route Ownership Rules
- **PRIVATE**: Only creator can see/modify
- **SHARED**: Same airline employees can see, only creator can modify
- **PUBLIC**: Everyone can see, only admin can modify (typically admin routes)

---

## 🔄 Real-time Features

### WebSocket Configuration
- **Endpoint**: `/ws`
- **Protocol**: SockJS + STOMP
- **Topics**:
    - `/topic/reference/updates` (all updates)
    - `/topic/reference/airlines` (airline-specific)
    - `/topic/reference/aircrafts` (aircraft-specific)
    - `/topic/reference/airports` (airport-specific)

### WebSocket Integration Example
```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    // Subscribe to all reference updates
    stompClient.subscribe('/topic/reference/updates', function(message) {
        const update = JSON.parse(message.body);
        console.log('Reference update:', update);
        // Handle real-time update in UI
    });
    
    // Subscribe to specific entity updates
    stompClient.subscribe('/topic/reference/airlines', function(message) {
        const airlineUpdate = JSON.parse(message.body);
        // Update airline data in real-time
    });
});
```

### Kafka Event Publishing
Events are published to `reference.events` topic with this format:
```json
{
  "eventId": "uuid",
  "eventType": "AIRLINE_CREATED|AIRLINE_UPDATED|AIRLINE_DELETED",
  "eventTime": "2025-07-27T10:30:00",
  "entityType": "AIRLINE|AIRPORT|AIRCRAFT|ROUTE|CREW_MEMBER",
  "entityId": "123",
  "payload": {
    "id": 123,
    "iataCode": "TK",
    "name": "Turkish Airlines",
    // ... entity data
  },
  "version": "1.0"
}
```

---

## 🔗 Integration Guide

### For Other Microservices

#### 1. Reference Data Consumption
```java
// Example service to consume reference data
@Service
public class ReferenceDataService {
    
    @Value("${reference.service.url}")
    private String referenceServiceUrl;
    
    public AirlineResponse getAirline(Long airlineId) {
        return restTemplate.getForObject(
            referenceServiceUrl + "/api/v1/airlines/" + airlineId,
            AirlineResponse.class
        );
    }
    
    public AirportResponse getAirportByIata(String iataCode) {
        return restTemplate.getForObject(
            referenceServiceUrl + "/api/v1/airports/iata/" + iataCode,
            AirportResponse.class
        );
    }
}
```

#### 2. Kafka Event Consumption
```java
@KafkaListener(topics = "reference.events")
public void handleReferenceEvent(ReferenceEvent event) {
    switch (event.getEntityType()) {
        case "AIRLINE":
            handleAirlineEvent(event);
            break;
        case "AIRPORT":
            handleAirportEvent(event);
            break;
        // Handle other entity types
    }
}
```

#### 3. Cache Invalidation Strategy
```java
@EventListener
public void handleReferenceUpdate(ReferenceEvent event) {
    // Invalidate relevant cache entries
    cacheManager.evict("airlines", event.getEntityId());
    // Notify other services if needed
}
```

### Required Dependencies
```xml
<dependencies>
    <!-- For REST calls -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- For Kafka consumption -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- For WebSocket (optional) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
</dependencies>
```

---

## 📊 Data Models

### Core Response DTOs

#### AirlineResponse
```java
public class AirlineResponse {
    private Long id;
    private String iataCode;        // TK, PC, SU
    private String icaoCode;        // THY, PGT, AFL
    private String name;            // Turkish Airlines
    private String country;         // Turkey
    private AirlineType type;       // FULL_SERVICE, LOW_COST, CARGO, CHARTER
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### RouteResponse (Multi-segment)
```java
public class RouteResponse {
    private Long id;
    private String routeCode;                    // TK-001
    private String routeName;                    // Istanbul-Ankara Route
    private Integer distance;                    // Total distance
    private Integer estimatedFlightTime;        // Total time
    private Boolean active;
    private RouteType routeType;                // DOMESTIC, INTERNATIONAL
    private Long createdByUserId;
    private String createdByUserName;
    private RouteVisibility visibility;         // PRIVATE, SHARED, PUBLIC
    private Long airlineId;
    private String airlineName;
    
    // Multi-segment specific
    private List<RouteSegmentResponse> segments;
    private Integer totalDistance;              // Calculated from segments
    private Integer totalEstimatedTime;        // Calculated from segments
    private String routePath;                  // "IST → ANK → IZM"
    private Integer segmentCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### RouteSegmentResponse
```java
public class RouteSegmentResponse {
    private Long id;
    private Integer segmentOrder;
    private AirportResponse originAirport;
    private AirportResponse destinationAirport;
    private Integer distance;
    private Integer estimatedFlightTime;
    private Boolean active;
    private String segmentPath;               // "IST → ANK"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Enums
```java
public enum AirlineType {
    FULL_SERVICE, LOW_COST, CARGO, CHARTER
}

public enum AirportType {
    INTERNATIONAL, DOMESTIC, CARGO, MILITARY
}

public enum RouteType {
    DOMESTIC, INTERNATIONAL, CONTINENTAL
}

public enum RouteVisibility {
    PRIVATE,    // Only creator can see
    SHARED,     // Same airline employees can see
    PUBLIC      // Everyone can see (admin routes)
}

public enum CrewType {
    CAPTAIN, FIRST_OFFICER, FLIGHT_ENGINEER, 
    PURSER, FLIGHT_ATTENDANT, CABIN_CREW
}
```

---

## ⚠️ Error Handling

### Standard Error Response
```json
{
  "timestamp": "2025-07-27T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "validationErrors": {
    "iataCode": "IATA code must be 2-3 characters",
    "name": "Name is required"
  }
}
```

### Common HTTP Status Codes
- **200**: Success
- **201**: Created
- **204**: No Content (successful delete)
- **400**: Bad Request (validation errors)
- **401**: Unauthorized (missing/invalid token)
- **403**: Forbidden (insufficient permissions)
- **404**: Not Found
- **409**: Conflict (duplicate resource)
- **500**: Internal Server Error

### Business Rule Validations
- **Duplicate Prevention**: IATA/ICAO codes must be unique
- **Dependency Checking**: Cannot delete airlines with active aircrafts/crew
- **Route Ownership**: Users can only modify their own routes (unless admin)
- **Route Validation**: Multi-segment routes must have valid airport sequences

---

## 🚀 Performance Considerations

### Database Optimization
- **Indexes**: Created on frequently queried columns
    - `airlines(iata_code, icao_code)`
    - `airports(iata_code, icao_code)`
    - `routes(route_code, created_by_user_id, airline_id)`
    - `route_segments(route_id, segment_order)`

### Pagination
- Default page size: 20
- Maximum recommended: 100
- Always use pagination for large datasets

### Caching Strategy
- **Reference Data**: Highly cacheable (airlines, airports rarely change)
- **Routes**: Cache with user context consideration
- **Cache Invalidation**: Via Kafka events

### Query Optimization
- **Lazy Loading**: JPA relationships use LAZY fetch
- **Projection**: DTOs prevent over-fetching
- **Batch Operations**: Available for bulk operations

---

## 🚀 Deployment Guide

### Environment Configuration

#### Development
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3308/reference_db
    username: admin
    password: 123456
  kafka:
    bootstrap-servers: localhost:9092
```

#### Production
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql-prod:3306/reference_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
jwt:
  secret: ${JWT_SECRET}
```

### Docker Configuration
```dockerfile
FROM openjdk:17-jre-slim
COPY target/reference-manager-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Health Checks
```yaml
# Available endpoints
GET /actuator/health     # Service health
GET /actuator/info       # Application info
GET /actuator/metrics    # Application metrics
```

### Migration Strategy
1. **Database**: Liquibase auto-runs on startup
2. **Zero Downtime**: Blue-green deployment supported
3. **Rollback**: Liquibase rollback scripts available

---

## 📈 Monitoring & Observability

### Metrics to Monitor
- **Response Time**: p95 < 300ms
- **Error Rate**: < 1%
- **Database Connection Pool**: Usage < 80%
- **Kafka Publish Rate**: Events/second
- **WebSocket Connections**: Active connections

### Logging
```yaml
logging:
  level:
    com.flightmanagement: DEBUG
    org.springframework.security: INFO
    org.springframework.kafka: INFO
```

### Health Check Response
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "kafka": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## 🔮 Future Enhancements

### Planned Features
1. **Audit Trail**: Complete change history
2. **Bulk Import/Export**: CSV operations for all entities
3. **Advanced Search**: Full-text search capabilities
4. **API Versioning**: Support for multiple API versions
5. **Rate Limiting**: Request throttling
6. **Data Validation**: External API integration for airport/airline validation

### Integration Roadmap
1. **Flight Service**: Route consumption
2. **Archive Service**: Reference data archiving
3. **Notification Service**: Real-time notifications
4. **User Management Service**: Enhanced user/role management

---

## 📞 Support & Contact

### Service Ownership
- **Team**: Flight Management Development Team
- **Repository**: `flight-management-system/reference-manager-service`
- **Documentation**: This file + API docs

### Getting Help
1. Check this documentation first
2. Review API endpoint documentation above
3. Check application logs for detailed error messages
4. Contact development team for architecture questions

### Contributing
1. Follow existing code patterns
2. Add tests for new features
3. Update documentation
4. Submit PR for review

---

*This documentation is automatically updated with each release. Last updated: July 27, 2025*