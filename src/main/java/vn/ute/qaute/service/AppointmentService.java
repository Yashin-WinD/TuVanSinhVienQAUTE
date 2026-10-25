package vn.ute.qaute.service;

import vn.ute.qaute.dto.request.CreateAppointmentDto;
import vn.ute.qaute.entity.Appointment;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.AppointmentStatus;

import java.util.List;

public interface AppointmentService {
    Appointment createAppointment(CreateAppointmentDto dto, User lecturer);
    List<Appointment> getAppointmentsByStudent(Long studentId);
    List<Appointment> getAppointmentsByStudentAndStatus(Long studentId, AppointmentStatus status);
    List<Appointment> getAppointmentsByLecturer(Long lecturerId);
    List<Appointment> getAppointmentsByLecturerAndStatus(Long lecturerId, AppointmentStatus status);
    void updateStatus(Long appointmentId, AppointmentStatus status);
    void cancelAppointment(Long appointmentId, String reason, User user);
    void confirmAppointment(Long appointmentId, User student);
}
