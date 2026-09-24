package vn.ute.qaute.service;

import vn.ute.qaute.entity.ConsultationField;
import vn.ute.qaute.entity.Department;

import java.util.List;

public interface DepartmentService {
    List<Department> getAllDepartments();
    Department getDepartmentById(Long id);
    Department createDepartment(Department department);
    List<ConsultationField> getAllFields();
    List<ConsultationField> getFieldsByDepartment(Long departmentId);
    ConsultationField getFieldById(Long id);
    ConsultationField createField(ConsultationField field);
}
