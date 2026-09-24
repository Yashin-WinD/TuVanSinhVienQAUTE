package vn.ute.qaute.service;

import vn.ute.qaute.dto.request.CreateAppointmentDto;
import vn.ute.qaute.entity.Appointment;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.AppointmentStatus;

import java.util.List;

public interface AppointmentService {
    Appointment createAppointment(CreateAppointmentDto dto, User lecturer);
    List<Appointment> getAppointmentsByStudent(Long studentId);
    List<Appointment> getAppointmentsByLecturer(Long lecturerId);
    void updateStatus(Long appointmentId, AppointmentStatus status);
}
