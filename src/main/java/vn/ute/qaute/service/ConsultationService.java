package vn.ute.qaute.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.ute.qaute.dto.request.AnswerRequestDto;
import vn.ute.qaute.dto.request.CreateConsultationDto;
import vn.ute.qaute.dto.request.FeedbackDto;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RequestStatus;

import java.util.List;

public interface ConsultationService {
    ConsultationRequest createRequest(CreateConsultationDto dto, User student);
    ConsultationRequest getRequestById(Long id);
    Page<ConsultationRequest> getRequestsByStudent(Long studentId, Pageable pageable);
    Page<ConsultationRequest> getRequestsByLecturer(Long lecturerId, Pageable pageable);
    Page<ConsultationRequest> getPendingRequestsByDepartment(Long departmentId, Pageable pageable);
    Page<ConsultationRequest> getAllRequests(Pageable pageable);

    void assignLecturer(Long requestId, Long lecturerId);
    void answerRequest(Long requestId, AnswerRequestDto dto, User lecturer);
    void rateConsultation(Long requestId, FeedbackDto dto, User student);
    void closeRequest(Long requestId);
    void updateStatus(Long requestId, RequestStatus status);

    // Bổ sung cho Sprint 1: Lọc, Chuyển tiếp & Quản trị phiếu
    Page<ConsultationRequest> filterRequests(Long studentId, Long lecturerId, Long departmentId, Long fieldId,
                                            RequestStatus status, vn.ute.qaute.entity.enums.PriorityLevel priority,
                                            String keyword, Pageable pageable);
    void forwardRequest(Long requestId, Long targetFieldId, Long targetLecturerId, String reason, User lecturer);
    void updatePriority(Long requestId, vn.ute.qaute.entity.enums.PriorityLevel priority);
    void toggleRequestStatus(Long requestId);

    long countByStatus(RequestStatus status);
    long countByLecturerAndStatus(Long lecturerId, RequestStatus status);
    long countByStudent(Long studentId);
}
