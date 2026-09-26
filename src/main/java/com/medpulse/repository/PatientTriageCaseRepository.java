package com.medpulse.repository;

import com.medpulse.model.PatientTriageCase;
import com.medpulse.model.TriageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientTriageCaseRepository extends JpaRepository<PatientTriageCase, Long> {
    Optional<PatientTriageCase> findByPatientId(String patientId);
    List<PatientTriageCase> findByTriageStatus(TriageStatus status);
    List<PatientTriageCase> findByDepartmentId(Long departmentId);
    List<PatientTriageCase> findByAdmittedBy_Id(java.util.UUID userId);
    List<PatientTriageCase> findByAssignedDoctor_Id(Long doctorId);

    @Query("SELECT c.triageStatus, COUNT(c) FROM PatientTriageCase c GROUP BY c.triageStatus")
    List<Object[]> countCasesByStatus();

    @Query("SELECT c.aiAcuityLevel, COUNT(c) FROM PatientTriageCase c WHERE c.aiAcuityLevel IS NOT NULL GROUP BY c.aiAcuityLevel")
    List<Object[]> countCasesByAcuityLevel();
}
