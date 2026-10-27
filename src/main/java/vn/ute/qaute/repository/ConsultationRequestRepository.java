package vn.ute.qaute.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.enums.RequestStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ute.qaute.entity.enums.PriorityLevel;

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

    @Query("SELECT r FROM ConsultationRequest r WHERE " +
           "(:studentId IS NULL OR r.student.id = :studentId) AND " +
           "(:lecturerId IS NULL OR r.lecturer.id = :lecturerId) AND " +
           "(:departmentId IS NULL OR r.field.department.id = :departmentId) AND " +
           "(:fieldId IS NULL OR r.field.id = :fieldId) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:priority IS NULL OR r.priority = :priority) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<ConsultationRequest> filterRequests(
            @Param("studentId") Long studentId,
            @Param("lecturerId") Long lecturerId,
            @Param("departmentId") Long departmentId,
            @Param("fieldId") Long fieldId,
            @Param("status") RequestStatus status,
            @Param("priority") PriorityLevel priority,
            @Param("keyword") String keyword,
            Pageable pageable);
}

