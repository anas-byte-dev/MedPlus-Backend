package com.medpulse.service;

import com.medpulse.dto.DepartmentDtos;
import com.medpulse.exception.BadRequestException;
import com.medpulse.exception.ResourceNotFoundException;
import com.medpulse.model.MedicalDepartment;
import com.medpulse.repository.MedicalDepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalDepartmentService {

    private final MedicalDepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDtos.DepartmentResponse getDepartmentById(Long id) {
        MedicalDepartment dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical Department not found with id: " + id));
        return mapToResponse(dept);
    }

    @Transactional
    public DepartmentDtos.DepartmentResponse createDepartment(DepartmentDtos.CreateDepartmentRequest request) {
        if (departmentRepository.findByCode(request.getCode()).isPresent()) {
            throw new BadRequestException("Department with code already exists: " + request.getCode());
        }

        MedicalDepartment dept = MedicalDepartment.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .specialty(request.getSpecialty())
                .headPhysician(request.getHeadPhysician())
                .bedCapacity(request.getBedCapacity())
                .currentOccupancy(0)
                .acuityLevel(request.getAcuityLevel())
                .description(request.getDescription())
                .clinicalProtocols(request.getClinicalProtocols())
                .build();

        return mapToResponse(departmentRepository.save(dept));
    }

    @Transactional
    public DepartmentDtos.DepartmentResponse updateOccupancy(Long id, int delta) {
        MedicalDepartment dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        int newOcc = Math.max(0, Math.min(dept.getBedCapacity(), dept.getCurrentOccupancy() + delta));
        dept.setCurrentOccupancy(newOcc);
        return mapToResponse(departmentRepository.save(dept));
    }

    public DepartmentDtos.DepartmentResponse mapToResponse(MedicalDepartment dept) {
        double rate = 0.0;
        if (dept.getBedCapacity() != null && dept.getBedCapacity() > 0) {
            rate = Math.round(((double) dept.getCurrentOccupancy() / dept.getBedCapacity()) * 1000.0) / 10.0;
        }

        return DepartmentDtos.DepartmentResponse.builder()
                .id(dept.getId())
                .name(dept.getName())
                .code(dept.getCode())
                .specialty(dept.getSpecialty())
                .headPhysician(dept.getHeadPhysician())
                .bedCapacity(dept.getBedCapacity())
                .currentOccupancy(dept.getCurrentOccupancy())
                .acuityLevel(dept.getAcuityLevel())
                .description(dept.getDescription())
                .clinicalProtocols(dept.getClinicalProtocols())
                .occupancyRate(rate)
                .createdAt(dept.getCreatedAt())
                .build();
    }
}
