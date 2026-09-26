package com.medpulse.service;

import com.medpulse.exception.ResourceNotFoundException;
import com.medpulse.model.Doctor;
import com.medpulse.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors(String city, String hospital, String specialty, String query) {
        String trimmedCity = (city != null && !city.isBlank() && !city.equalsIgnoreCase("ALL")) ? city.trim() : null;
        String trimmedHospital = (hospital != null && !hospital.isBlank() && !hospital.equalsIgnoreCase("ALL")) ? hospital.trim() : null;
        String trimmedSpecialty = (specialty != null && !specialty.isBlank() && !specialty.equalsIgnoreCase("ALL")) ? specialty.trim() : null;
        String trimmedQuery = (query != null && !query.isBlank()) ? query.trim() : null;

        List<Doctor> all = doctorRepository.findAllByOrderByRatingDesc();

        if (trimmedCity == null && trimmedHospital == null && trimmedSpecialty == null && trimmedQuery == null) {
            return all;
        }

        return all.stream()
                .filter(d -> trimmedCity == null || (d.getCity() != null && d.getCity().equalsIgnoreCase(trimmedCity)))
                .filter(d -> trimmedHospital == null || (d.getHospital() != null && d.getHospital().toLowerCase().contains(trimmedHospital.toLowerCase())))
                .filter(d -> trimmedSpecialty == null || (d.getSpecialty() != null && d.getSpecialty().toLowerCase().contains(trimmedSpecialty.toLowerCase())))
                .filter(d -> trimmedQuery == null || (
                        (d.getName() != null && d.getName().toLowerCase().contains(trimmedQuery.toLowerCase())) ||
                        (d.getSpecialty() != null && d.getSpecialty().toLowerCase().contains(trimmedQuery.toLowerCase())) ||
                        (d.getHospital() != null && d.getHospital().toLowerCase().contains(trimmedQuery.toLowerCase())) ||
                        (d.getAddress() != null && d.getAddress().toLowerCase().contains(trimmedQuery.toLowerCase())) ||
                        (d.getCity() != null && d.getCity().toLowerCase().contains(trimmedQuery.toLowerCase()))
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialist Doctor not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<String> getCities() {
        return doctorRepository.findDistinctCities();
    }

    @Transactional(readOnly = true)
    public List<String> getHospitals() {
        return doctorRepository.findDistinctHospitals();
    }

    @Transactional(readOnly = true)
    public List<String> getSpecialties() {
        return doctorRepository.findDistinctSpecialties();
    }

    @Transactional
    public Doctor createDoctor(Doctor doctor) {
        if (doctor.getRating() == null) doctor.setRating(4.8);
        if (doctor.getReviewCount() == null) doctor.setReviewCount(24);
        if (doctor.getVerified() == null) doctor.setVerified(true);
        return doctorRepository.save(doctor);
    }
}
