package com.medpulse.repository;

import com.medpulse.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findAllByCityIgnoreCase(String city);

    List<Doctor> findAllByHospitalIgnoreCase(String hospital);

    List<Doctor> findAllBySpecialtyContainingIgnoreCase(String specialty);

    List<Doctor> findAllByOrderByRatingDesc();

    @Query("SELECT DISTINCT d.city FROM Doctor d ORDER BY d.city")
    List<String> findDistinctCities();

    @Query("SELECT DISTINCT d.hospital FROM Doctor d ORDER BY d.hospital")
    List<String> findDistinctHospitals();

    @Query("SELECT DISTINCT d.specialty FROM Doctor d ORDER BY d.specialty")
    List<String> findDistinctSpecialties();
}
