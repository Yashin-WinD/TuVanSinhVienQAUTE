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

    @Autowired
    public ConsultationServiceImpl(ConsultationRequestRepository requestRepository,
                                   ConsultationFieldRepository fieldRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.fieldRepository = fieldRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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

        // Thông báo cho sinh viên nhận câu trả lời
        notificationService.sendNotification(
                request.getStudent(),
                "Câu hỏi của bạn đã có lời giải đáp!",
                "Thầy/Cô " + lecturer.getFullName() + " đã trả lời câu hỏi: '" + request.getTitle() + "'",
                "/student/requests/" + request.getId()
        );
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
