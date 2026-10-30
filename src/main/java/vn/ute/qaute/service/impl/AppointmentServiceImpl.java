package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.dto.request.CreateAppointmentDto;
import vn.ute.qaute.entity.Appointment;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.AppointmentStatus;
import vn.ute.qaute.entity.enums.RequestStatus;
import vn.ute.qaute.entity.enums.RoleType;
import vn.ute.qaute.exception.BadRequestException;
import vn.ute.qaute.exception.ResourceNotFoundException;
import vn.ute.qaute.repository.AppointmentRepository;
import vn.ute.qaute.repository.ConsultationRequestRepository;
import vn.ute.qaute.service.AppointmentService;
import vn.ute.qaute.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRequestRepository requestRepository;
    private final NotificationService notificationService;
    private final vn.ute.qaute.service.MailService mailService;

    @Autowired
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
            ConsultationRequestRepository requestRepository,
            NotificationService notificationService,
            vn.ute.qaute.service.MailService mailService) {
        this.appointmentRepository = appointmentRepository;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public Appointment createAppointment(CreateAppointmentDto dto, User lecturer) {
        if (dto.getMeetingTime() == null) {
            throw new BadRequestException("Thời gian hẹn không được để trống!");
        }
        if (dto.getMeetingTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Thời gian hẹn không được bé hơn thời gian hiện tại!");
        }

        ConsultationRequest consultation = requestRepository.findById(dto.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy phiếu tư vấn với ID: " + dto.getConsultationId()));

        if (consultation.getStatus() == RequestStatus.CLOSED) {
            throw new BadRequestException("Không thể đặt lịch hẹn cho phiếu tư vấn đã đóng!");
        }

        if (consultation.getLecturer() == null) {
            consultation.setLecturer(lecturer);
            if (consultation.getStatus() == RequestStatus.PENDING) {
                consultation.setStatus(RequestStatus.ASSIGNED);
            }
            requestRepository.save(consultation);
        }

        // Chống spam: Kiểm tra xem phiếu tư vấn này đã có lịch hẹn đang chờ hoặc cùng
        // khung giờ chưa
        List<Appointment> existingAppointments = appointmentRepository.findByConsultationId(dto.getConsultationId());
        boolean hasDuplicate = existingAppointments.stream().anyMatch(a -> a.getStatus() == AppointmentStatus.SCHEDULED
                && a.getMeetingTime() != null
                && Math.abs(java.time.Duration.between(a.getMeetingTime(), dto.getMeetingTime()).toMinutes()) < 30);
        if (hasDuplicate) {
            throw new BadRequestException(
                    "Phiếu tư vấn này đã có lịch hẹn đang lên lịch hoặc gần khung giờ này! Vui lòng không gửi yêu cầu trùng lặp.");
        }

        Appointment appointment = new Appointment(
                consultation,
                consultation.getStudent(),
                lecturer,
                dto.getMeetingTime(),
                dto.getLocationOrLink(),
                dto.getNote());

        Appointment saved = appointmentRepository.save(appointment);

        // Báo cho sinh viên về lịch hẹn mới trong hệ thống
        notificationService.sendNotification(
                consultation.getStudent(),
                "Bạn có lịch hẹn tư vấn mới!",
                "Giảng viên " + lecturer.getFullName() + " đã đặt lịch hẹn lúc: " + dto.getMeetingTime().toString()
                        + " tại: " + dto.getLocationOrLink(),
                "/student/appointments");

        // Gửi email tự động cho sinh viên
        if (consultation.getStudent().getEmail() != null) {
            String timeStr = dto.getMeetingTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"));
            mailService.sendNotificationEmail(
                    consultation.getStudent().getEmail(),
                    "Lịch hẹn tư vấn mới với Thầy/Cô " + lecturer.getFullName(),
                    "Thông báo lịch hẹn tư vấn học tập",
                    "Thầy/Cô " + lecturer.getFullName() + " đã lên lịch hẹn tư vấn với bạn.\n\n"
                            + "• Thời gian: " + timeStr + "\n"
                            + "• Địa điểm/Link: " + dto.getLocationOrLink() + "\n"
                            + "• Ghi chú: " + (dto.getNote() != null ? dto.getNote() : "Không có") + "\n\n"
                            + "Vui lòng đăng nhập vào hệ thống để xác nhận hoàn thành hoặc xin dời lịch nếu cần.",
                    "/student/appointments");
        }

        return saved;
    }

    @Override
    public List<Appointment> getAppointmentsByStudent(Long studentId) {
        return appointmentRepository.findByStudentIdOrderByMeetingTimeDesc(studentId);
    }

    @Override
    public List<Appointment> getAppointmentsByStudentAndStatus(Long studentId, AppointmentStatus status) {
        if (status == null) {
            return getAppointmentsByStudent(studentId);
        }
        return appointmentRepository.findByStudentIdAndStatusOrderByMeetingTimeDesc(studentId, status);
    }

    @Override
    public List<Appointment> getAppointmentsByLecturer(Long lecturerId) {
        return appointmentRepository.findByLecturerIdOrderByMeetingTimeDesc(lecturerId);
    }

    @Override
    public List<Appointment> getAppointmentsByLecturerAndStatus(Long lecturerId, AppointmentStatus status) {
        if (status == null) {
            return getAppointmentsByLecturer(lecturerId);
        }
        return appointmentRepository.findByLecturerIdAndStatusOrderByMeetingTimeDesc(lecturerId, status);
    }

    @Override
    @Transactional
    public void updateStatus(Long appointmentId, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void confirmAppointment(Long appointmentId, User user) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));

        boolean isStudent = appointment.getStudent() != null && appointment.getStudent().getId().equals(user.getId());
        boolean isLecturer = appointment.getLecturer() != null && appointment.getLecturer().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == RoleType.ROLE_ADMIN;

        if (!isStudent && !isLecturer && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền xác nhận hoàn thành lịch hẹn này!");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        String timeStr = appointment.getMeetingTime() != null
                ? appointment.getMeetingTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                : "";

        if (isLecturer || isAdmin) {
            // Thông báo cho Sinh viên khi Giảng viên xác nhận hoàn thành lịch hẹn
            notificationService.sendNotification(
                    appointment.getStudent(),
                    "Buổi hẹn tư vấn đã hoàn thành",
                    "Thầy/Cô " + user.getFullName() + " đã xác nhận hoàn thành lịch hẹn tư vấn lúc " + timeStr,
                    "/student/appointments");
        } else {
            // Thông báo cho Giảng viên khi Sinh viên xác nhận hoàn thành lịch hẹn
            notificationService.sendNotification(
                    appointment.getLecturer(),
                    "Sinh viên đã hoàn tất/xác nhận lịch hẹn",
                    "Sinh viên " + user.getFullName() + " đã xác nhận hoàn thành lịch hẹn lúc " + timeStr,
                    "/lecturer/schedule");
        }
    }

    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId, String reason, User user) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));

        boolean isStudent = appointment.getStudent().getId().equals(user.getId());
        boolean isLecturer = appointment.getLecturer().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == RoleType.ROLE_ADMIN;

        if (!isStudent && !isLecturer && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền hủy lịch hẹn này!");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        appointmentRepository.save(appointment);

        User targetUser = isStudent ? appointment.getLecturer() : appointment.getStudent();
        String cancelBy = isStudent ? ("Sinh viên " + user.getFullName()) : ("Giảng viên " + user.getFullName());

        notificationService.sendNotification(
                targetUser,
                "Lịch hẹn tư vấn đã bị hủy/xin dời lịch",
                cancelBy + " đã hủy lịch hẹn. Lý do: " + (reason != null ? reason : "Không có lý do cụ thể"),
                isStudent ? "/lecturer/schedule" : "/student/appointments");

        if (targetUser.getEmail() != null) {
            mailService.sendNotificationEmail(
                    targetUser.getEmail(),
                    "Thông báo hủy / xin dời lịch hẹn tư vấn",
                    "Lịch hẹn đã bị hủy hoặc xin dời",
                    cancelBy + " đã thông báo hủy/xin dời lịch hẹn vào ngày " + appointment.getMeetingTime().toString()
                            + ".\nLý do: " + (reason != null ? reason : "Không có lý do cụ thể") + "\n\n"
                            + "Vui lòng kiểm tra lại lịch trên hệ thống.",
                    isStudent ? "/lecturer/schedule" : "/student/appointments");
        }
    }
}
