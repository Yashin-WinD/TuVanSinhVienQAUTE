package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.dto.request.CreateAppointmentDto;
import vn.ute.qaute.entity.Appointment;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.AppointmentStatus;
import vn.ute.qaute.exception.ResourceNotFoundException;
import vn.ute.qaute.repository.AppointmentRepository;
import vn.ute.qaute.repository.ConsultationRequestRepository;
import vn.ute.qaute.service.AppointmentService;
import vn.ute.qaute.service.NotificationService;

import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRequestRepository requestRepository;
    private final NotificationService notificationService;

    @Autowired
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  ConsultationRequestRepository requestRepository,
                                  NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Appointment createAppointment(CreateAppointmentDto dto, User lecturer) {
        ConsultationRequest consultation = requestRepository.findById(dto.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu tư vấn"));

        Appointment appointment = new Appointment(
                consultation,
                consultation.getStudent(),
                lecturer,
                dto.getMeetingTime(),
                dto.getLocationOrLink(),
                dto.getNote()
        );

        Appointment saved = appointmentRepository.save(appointment);

        // Báo cho sinh viên về lịch hẹn mới
        notificationService.sendNotification(
                consultation.getStudent(),
                "Bạn có lịch hẹn tư vấn mới!",
                "Giảng viên " + lecturer.getFullName() + " đã đặt lịch hẹn lúc: " + dto.getMeetingTime().toString()
                        + " tại: " + dto.getLocationOrLink(),
                "/student/requests/" + consultation.getId()
        );

        return saved;
    }

    @Override
    public List<Appointment> getAppointmentsByStudent(Long studentId) {
        return appointmentRepository.findByStudentIdOrderByMeetingTimeDesc(studentId);
    }

    @Override
    public List<Appointment> getAppointmentsByLecturer(Long lecturerId) {
        return appointmentRepository.findByLecturerIdOrderByMeetingTimeDesc(lecturerId);
    }

    @Override
    @Transactional
    public void updateStatus(Long appointmentId, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }
}
