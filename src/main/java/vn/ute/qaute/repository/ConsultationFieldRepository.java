package vn.ute.qaute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ute.qaute.entity.ConsultationField;

import java.util.List;

@Repository
public interface ConsultationFieldRepository extends JpaRepository<ConsultationField, Long> {
    List<ConsultationField> findByDepartmentId(Long departmentId);
}
