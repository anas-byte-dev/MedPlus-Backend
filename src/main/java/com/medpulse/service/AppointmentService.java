package com.medpulse.service;

import com.medpulse.dto.AppointmentDtos.AppointmentResponse;
import com.medpulse.dto.AppointmentDtos.BookAppointmentRequest;
import com.medpulse.exception.ResourceNotFoundException;
import com.medpulse.model.Appointment;
import com.medpulse.model.Doctor;
import com.medpulse.repository.AppointmentRepository;
import com.medpulse.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialist Doctor not found with ID: " + request.getDoctorId()));

        String reference = generateBookingReference();

        String chiefComplaint = request.getSymptoms();
        if (chiefComplaint == null || chiefComplaint.isBlank()) {
            if (request.getReasonForVisit() != null && !request.getReasonForVisit().isBlank()) {
                chiefComplaint = request.getReasonForVisit().trim();
            } else if (request.getDescription() != null && !request.getDescription().isBlank()) {
                chiefComplaint = request.getDescription().trim();
            }
        }

        Appointment appointment = Appointment.builder()
                .bookingReference(reference)
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                .doctorSpecialty(doctor.getSpecialty())
                .doctorQualifications(doctor.getQualifications())
                .doctorDesignation(doctor.getDesignation())
                .hospital(doctor.getHospital())
                .city(doctor.getCity())
                .address(doctor.getAddress())
                .patientName(request.getPatientName().trim())
                .patientPhone(request.getPatientPhone().trim())
                .patientEmail(request.getPatientEmail() != null ? request.getPatientEmail().trim() : null)
                .patientAge(request.getPatientAge())
                .patientGender(request.getPatientGender())
                .appointmentDate(request.getAppointmentDate())
                .timeSlot(request.getTimeSlot())
                .symptoms(chiefComplaint != null ? chiefComplaint.trim() : null)
                .status("CONFIRMED")
                .consultationFee(doctor.getConsultationFee() != null ? doctor.getConsultationFee() : 500)
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Clinical appointment booked: ref={}, doctor={}, patient={}", reference, doctor.getName(), saved.getPatientName());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(String phone, String email, Long doctorId, String hospital) {
        if (doctorId != null) {
            return appointmentRepository.findAllByDoctorIdOrderByAppointmentDateDesc(doctorId).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        if (hospital != null && !hospital.isBlank()) {
            return appointmentRepository.findAllByHospitalContainingIgnoreCaseOrderByAppointmentDateDesc(hospital.trim()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        if (email != null && !email.isBlank()) {
            return appointmentRepository.findAllByPatientEmailOrderByAppointmentDateDesc(email.trim()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        if (phone != null && !phone.isBlank()) {
            return appointmentRepository.findAllByPatientPhoneOrderByAppointmentDateDesc(phone.trim()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return getAllAppointments();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentDateDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatientPhone(String phone) {
        return appointmentRepository.findAllByPatientPhoneOrderByAppointmentDateDesc(phone).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getByBookingReference(String ref) {
        Appointment apt = appointmentRepository.findByBookingReference(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with booking reference: " + ref));
        return mapToResponse(apt);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, String status) {
        Appointment apt = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));
        apt.setStatus(status.toUpperCase().trim());
        Appointment saved = appointmentRepository.save(apt);
        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long id) {
        return updateStatus(id, "CANCELLED");
    }

    private String generateBookingReference() {
        return "MED-APT-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000));
    }

    private AppointmentResponse mapToResponse(Appointment apt) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedTime = apt.getCreatedAt() != null ? apt.getCreatedAt().format(dtf) : "Just now";

        String confirmationMsg = String.format("Appointment confirmed with %s at %s (%s) on %s at %s. Please arrive 15 minutes before your consultation.",
                apt.getDoctorName(), apt.getHospital(), apt.getCity(), apt.getAppointmentDate(), apt.getTimeSlot());

        // Fetch live doctor qualification and designation for real-time PDF generation
        String qualifications = apt.getDoctorQualifications();
        String designation = apt.getDoctorDesignation();
        if ((qualifications == null || designation == null) && apt.getDoctorId() != null) {
            var docOpt = doctorRepository.findById(apt.getDoctorId());
            if (docOpt.isPresent()) {
                var doc = docOpt.get();
                if (qualifications == null) qualifications = doc.getQualifications();
                if (designation == null) designation = doc.getDesignation();
            }
        }

        return AppointmentResponse.builder()
                .id(apt.getId())
                .bookingReference(apt.getBookingReference())
                .doctorId(apt.getDoctorId())
                .doctorName(apt.getDoctorName())
                .doctorSpecialty(apt.getDoctorSpecialty())
                .doctorQualifications(qualifications)
                .doctorDesignation(designation)
                .hospital(apt.getHospital())
                .city(apt.getCity())
                .address(apt.getAddress())
                .patientName(apt.getPatientName())
                .patientPhone(apt.getPatientPhone())
                .patientEmail(apt.getPatientEmail())
                .patientAge(apt.getPatientAge())
                .patientGender(apt.getPatientGender())
                .appointmentDate(apt.getAppointmentDate())
                .timeSlot(apt.getTimeSlot())
                .symptoms(apt.getSymptoms())
                .reasonForVisit(apt.getSymptoms())
                .description(apt.getSymptoms())
                .status(apt.getStatus())
                .consultationFee(apt.getConsultationFee())
                .formattedBookingTime(formattedTime)
                .confirmationMessage(confirmationMsg)
                .build();
    }
}
