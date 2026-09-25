package vn.ute.qaute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ute.qaute.entity.Appointment;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByStudentIdOrderByMeetingTimeDesc(Long studentId);
    List<Appointment> findByLecturerIdOrderByMeetingTimeDesc(Long lecturerId);
    List<Appointment> findByConsultationId(Long consultationId);
}
