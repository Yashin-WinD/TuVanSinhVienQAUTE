package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.entity.ConsultationField;
import vn.ute.qaute.entity.Department;
import vn.ute.qaute.exception.ResourceNotFoundException;
import vn.ute.qaute.repository.ConsultationFieldRepository;
import vn.ute.qaute.repository.DepartmentRepository;
import vn.ute.qaute.service.DepartmentService;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ConsultationFieldRepository fieldRepository;

    @Autowired
    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 ConsultationFieldRepository fieldRepository) {
        this.departmentRepository = departmentRepository;
        this.fieldRepository = fieldRepository;
    }

    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban với ID: " + id));
    }

    @Override
    @Transactional
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public List<ConsultationField> getAllFields() {
        return fieldRepository.findAll();
    }

    @Override
    public List<ConsultationField> getFieldsByDepartment(Long departmentId) {
        return fieldRepository.findByDepartmentId(departmentId);
    }

    @Override
    public ConsultationField getFieldById(Long id) {
        return fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lĩnh vực tư vấn với ID: " + id));
    }

    @Override
    @Transactional
    public ConsultationField createField(ConsultationField field) {
        return fieldRepository.save(field);
    }
}
