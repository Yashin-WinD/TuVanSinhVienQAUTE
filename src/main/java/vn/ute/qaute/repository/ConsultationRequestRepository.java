package vn.ute.qaute.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.enums.RequestStatus;

import java.util.List;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
    Page<ConsultationRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);
    Page<ConsultationRequest> findByLecturerIdOrderByCreatedAtDesc(Long lecturerId, Pageable pageable);
    Page<ConsultationRequest> findByFieldDepartmentIdAndStatusOrderByCreatedAtDesc(Long departmentId, RequestStatus status, Pageable pageable);
    List<ConsultationRequest> findByStatus(RequestStatus status);
    long countByStatus(RequestStatus status);
    long countByLecturerIdAndStatus(Long lecturerId, RequestStatus status);
    long countByStudentId(Long studentId);
}
