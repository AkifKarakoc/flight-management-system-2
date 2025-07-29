package com.flightmanagement.referencemanagerservice.repository;

import com.flightmanagement.referencemanagerservice.entity.CrewMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {
    List<CrewMember> findByAirlineId(Long airlineId);
    boolean existsByEmployeeNumber(String employeeNumber);
    long countByAirlineId(Long airlineId);
}