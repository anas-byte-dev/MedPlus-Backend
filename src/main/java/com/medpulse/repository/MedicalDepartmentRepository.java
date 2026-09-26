package com.medpulse.repository;

import com.medpulse.model.MedicalDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalDepartmentRepository extends JpaRepository<MedicalDepartment, Long> {
    Optional<MedicalDepartment> findByCode(String code);
    Optional<MedicalDepartment> findByName(String name);
}
