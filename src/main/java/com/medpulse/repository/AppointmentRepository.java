package com.medpulse.repository;

import com.medpulse.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByBookingReference(String bookingReference);

    List<Appointment> findAllByPatientPhoneOrderByAppointmentDateDesc(String patientPhone);

    List<Appointment> findAllByPatientEmailOrderByAppointmentDateDesc(String patientEmail);

    List<Appointment> findAllByHospitalContainingIgnoreCaseOrderByAppointmentDateDesc(String hospital);

    List<Appointment> findAllByDoctorIdOrderByAppointmentDateDesc(Long doctorId);

    List<Appointment> findAllByOrderByAppointmentDateDesc();

    List<Appointment> findAllByAppointmentDateAndDoctorId(LocalDate appointmentDate, Long doctorId);
}
