package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.dto.request.AnswerRequestDto;
import vn.ute.qaute.dto.request.CreateConsultationDto;
import vn.ute.qaute.dto.request.FeedbackDto;
import vn.ute.qaute.entity.ConsultationField;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RequestStatus;
import vn.ute.qaute.exception.BadRequestException;
import vn.ute.qaute.exception.ResourceNotFoundException;
import vn.ute.qaute.repository.ConsultationFieldRepository;
import vn.ute.qaute.repository.ConsultationRequestRepository;
import vn.ute.qaute.repository.UserRepository;
import vn.ute.qaute.service.ConsultationService;
import vn.ute.qaute.service.NotificationService;

import java.time.LocalDateTime;

@Service
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRequestRepository requestRepository;
    private final ConsultationFieldRepository fieldRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final vn.ute.qaute.service.MailService mailService;

    @Autowired
    public ConsultationServiceImpl(ConsultationRequestRepository requestRepository,
                                   ConsultationFieldRepository fieldRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService,
                                   vn.ute.qaute.service.MailService mailService) {
        this.requestRepository = requestRepository;
        this.fieldRepository = fieldRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public ConsultationRequest createRequest(CreateConsultationDto dto, User student) {
        ConsultationField field = fieldRepository.findById(dto.getFieldId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lĩnh vực tư vấn với ID: " + dto.getFieldId()));

        ConsultationRequest request = new ConsultationRequest(
                dto.getTitle(),
                dto.getContent(),
                dto.getAttachmentUrls(),
                student,
                field,
                dto.getPriority()
        );

        ConsultationRequest saved = requestRepository.save(request);

        // Thông báo cho sinh viên
        notificationService.sendNotification(
                student,
                "Tạo phiếu tư vấn thành công",
                "Câu hỏi '" + saved.getTitle() + "' của bạn đã được gửi đến ban cố vấn.",
                "/student/requests/" + saved.getId()
        );

        // Auto-Mailer: Gửi email thông báo cho sinh viên xác nhận đã nhận câu hỏi
        if (student.getEmail() != null) {
            mailService.sendNotificationEmail(
                    student.getEmail(),
                    "Xác nhận gửi câu hỏi tư vấn #" + saved.getId(),
                    "Câu hỏi tư vấn của bạn đã được tiếp nhận",
                    "Chào " + student.getFullName() + ",\n\nCâu hỏi: '" + saved.getTitle() + "' thuộc Khoa/Ban: " 
                            + field.getDepartment().getName() + " đã được gửi lên hệ thống.\n"
                            + "Các giảng viên phụ trách sẽ xem xét và phản hồi sớm nhất có thể.\n\n"
                            + "Trân trọng,\nBan Tư vấn QAUTE",
                    "/student/requests/" + saved.getId()
            );
        }

        return saved;
    }

    @Override
    public ConsultationRequest getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu tư vấn với ID: " + id));
    }

    @Override
    public Page<ConsultationRequest> getRequestsByStudent(Long studentId, Pageable pageable) {
        return requestRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable);
    }

    @Override
    public Page<ConsultationRequest> getRequestsByLecturer(Long lecturerId, Pageable pageable) {
        return requestRepository.findByLecturerIdOrderByCreatedAtDesc(lecturerId, pageable);
    }

    @Override
    public Page<ConsultationRequest> getPendingRequestsByDepartment(Long departmentId, Pageable pageable) {
        return requestRepository.findByFieldDepartmentIdAndStatusOrderByCreatedAtDesc(
                departmentId, RequestStatus.PENDING, pageable);
    }

    @Override
    public Page<ConsultationRequest> getAllRequests(Pageable pageable) {
        return requestRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public void assignLecturer(Long requestId, Long lecturerId) {
        ConsultationRequest request = getRequestById(requestId);
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên với ID: " + lecturerId));

        request.setLecturer(lecturer);
        request.setStatus(RequestStatus.ASSIGNED);
        requestRepository.save(request);

        // Thông báo cho giảng viên
        notificationService.sendNotification(
                lecturer,
                "Bạn được phân công câu hỏi mới",
                "Phiếu tư vấn: '" + request.getTitle() + "' từ sinh viên " + request.getStudent().getFullName(),
                "/lecturer/requests/" + request.getId() + "/answer"
        );
    }

    @Override
    @Transactional
    public void answerRequest(Long requestId, AnswerRequestDto dto, User lecturer) {
        ConsultationRequest request = getRequestById(requestId);
        request.setLecturer(lecturer);
        request.setAnswerContent(dto.getAnswerContent());
        if (dto.getAnswerFiles() != null && !dto.getAnswerFiles().trim().isEmpty()) {
            request.setAnswerFiles(dto.getAnswerFiles());
        }
        request.setStatus(RequestStatus.RESOLVED);
        request.setResolvedAt(LocalDateTime.now());
        requestRepository.save(request);

        // Thông báo cho sinh viên nhận câu trả lời trong hệ thống
        notificationService.sendNotification(
                request.getStudent(),
                "Câu hỏi của bạn đã có lời giải đáp!",
                "Thầy/Cô " + lecturer.getFullName() + " đã trả lời câu hỏi: '" + request.getTitle() + "'",
                "/student/requests/" + request.getId()
        );

        // Auto-Mailer: Gửi email kèm trích dẫn câu trả lời cho sinh viên
        if (request.getStudent().getEmail() != null) {
            String answerSnippet = dto.getAnswerContent().length() > 200 
                    ? (dto.getAnswerContent().substring(0, 200) + "...") 
                    : dto.getAnswerContent();
            mailService.sendNotificationEmail(
                    request.getStudent().getEmail(),
                    "Câu hỏi #" + request.getId() + " đã được giải đáp",
                    "Thầy/Cô " + lecturer.getFullName() + " đã phản hồi câu hỏi của bạn",
                    "Tiêu đề câu hỏi: " + request.getTitle() + "\n\n"
                            + "Trích đoạn giải đáp:\n\"" + answerSnippet + "\"\n\n"
                            + "Vui lòng bấm vào liên kết bên dưới để xem toàn bộ nội dung phản hồi chi tiết và đánh giá mức độ hài lòng.",
                    "/student/requests/" + request.getId()
            );
        }
    }

    @Override
    @Transactional
    public void rateConsultation(Long requestId, FeedbackDto dto, User student) {
        ConsultationRequest request = getRequestById(requestId);
        if (!request.getStudent().getId().equals(student.getId())) {
            throw new BadRequestException("Bạn không có quyền đánh giá câu hỏi này");
        }

        request.setRating(dto.getRating());
        request.setFeedback(dto.getFeedback());
        request.setStatus(RequestStatus.CLOSED);
        requestRepository.save(request);
    }

    @Override
    @Transactional
    public void closeRequest(Long requestId) {
        ConsultationRequest request = getRequestById(requestId);
        request.setStatus(RequestStatus.CLOSED);
        requestRepository.save(request);
    }

    @Override
    @Transactional
    public void updateStatus(Long requestId, RequestStatus status) {
        ConsultationRequest request = getRequestById(requestId);
        request.setStatus(status);
        requestRepository.save(request);
    }

    @Override
    public Page<ConsultationRequest> filterRequests(Long studentId, Long lecturerId, Long departmentId, Long fieldId,
                                                    RequestStatus status, vn.ute.qaute.entity.enums.PriorityLevel priority,
                                                    String keyword, Pageable pageable) {
        return requestRepository.filterRequests(studentId, lecturerId, departmentId, fieldId, status, priority, keyword, pageable);
    }

    @Override
    @Transactional
    public void forwardRequest(Long requestId, Long targetFieldId, Long targetLecturerId, String reason, User lecturer) {
        ConsultationRequest request = getRequestById(requestId);
        ConsultationField targetField = fieldRepository.findById(targetFieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lĩnh vực chuyển tiếp"));

        request.setField(targetField);
        request.setForwardReason(reason);
        request.setForwardedFrom(lecturer);
        request.setForwardedAt(LocalDateTime.now());

        if (targetLecturerId != null) {
            User targetLecturer = userRepository.findById(targetLecturerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên đích"));
            request.setLecturer(targetLecturer);
            request.setStatus(RequestStatus.ASSIGNED);

            notificationService.sendNotification(
                    targetLecturer,
                    "Phiếu tư vấn được chuyển tiếp tới bạn",
                    "Thầy/Cô " + lecturer.getFullName() + " đã chuyển tiếp phiếu #" + request.getId() + " với lý do: " + reason,
                    "/lecturer/requests/" + request.getId() + "/answer"
            );

            if (targetLecturer.getEmail() != null) {
                mailService.sendNotificationEmail(
                        targetLecturer.getEmail(),
                        "Phiếu tư vấn #" + request.getId() + " được chuyển tiếp tới bạn",
                        "Bạn nhận được phiếu tư vấn chuyển tiếp",
                        "Thầy/Cô " + lecturer.getFullName() + " đã chuyển tiếp phiếu tư vấn #" + request.getId() 
                                + ": '" + request.getTitle() + "' đến bạn.\n\n"
                                + "Lý do chuyển tiếp: " + reason,
                        "/lecturer/requests/" + request.getId() + "/answer"
                );
            }
        } else {
            request.setLecturer(null);
            request.setStatus(RequestStatus.PENDING);
        }

        requestRepository.save(request);

        // Thông báo cho sinh viên biết câu hỏi đã được chuyển tiếp
        notificationService.sendNotification(
                request.getStudent(),
                "Câu hỏi của bạn đã được chuyển tiếp chuyên môn",
                "Câu hỏi #" + request.getId() + " đã được chuyển sang lĩnh vực: " + targetField.getName() + " để hỗ trợ chính xác hơn.",
                "/student/requests/" + request.getId()
        );
    }

    @Override
    @Transactional
    public void updatePriority(Long requestId, vn.ute.qaute.entity.enums.PriorityLevel priority) {
        ConsultationRequest request = getRequestById(requestId);
        request.setPriority(priority);
        requestRepository.save(request);
    }

    @Override
    @Transactional
    public void toggleRequestStatus(Long requestId) {
        ConsultationRequest request = getRequestById(requestId);
        if (request.getStatus() == RequestStatus.CLOSED) {
            request.setStatus(request.getAnswerContent() != null ? RequestStatus.RESOLVED : RequestStatus.ASSIGNED);
        } else {
            request.setStatus(RequestStatus.CLOSED);
        }
        requestRepository.save(request);
    }

    @Override
    public long countByStatus(RequestStatus status) {
        return requestRepository.countByStatus(status);
    }

    @Override
    public long countByLecturerAndStatus(Long lecturerId, RequestStatus status) {
        return requestRepository.countByLecturerIdAndStatus(lecturerId, status);
    }

    @Override
    public long countByStudent(Long studentId) {
        return requestRepository.countByStudentId(studentId);
    }
}
