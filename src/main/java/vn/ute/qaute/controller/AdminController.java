package vn.ute.qaute.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.ute.qaute.entity.ConsultationField;
import vn.ute.qaute.entity.Department;
import vn.ute.qaute.entity.Faq;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RequestStatus;
import vn.ute.qaute.entity.enums.RoleType;
import vn.ute.qaute.service.ConsultationService;
import vn.ute.qaute.service.DepartmentService;
import vn.ute.qaute.service.FaqService;
import vn.ute.qaute.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ConsultationService consultationService;
    private final DepartmentService departmentService;
    private final FaqService faqService;

    @Autowired
    public AdminController(UserService userService,
                           ConsultationService consultationService,
                           DepartmentService departmentService,
                           FaqService faqService) {
        this.userService = userService;
        this.consultationService = consultationService;
        this.departmentService = departmentService;
        this.faqService = faqService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("currentUser", currentUser);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("studentCount", userService.countUsersByRole(RoleType.ROLE_STUDENT));
        model.addAttribute("lecturerCount", userService.countUsersByRole(RoleType.ROLE_LECTURER));
        model.addAttribute("pendingCount", consultationService.countByStatus(RequestStatus.PENDING));
        model.addAttribute("resolvedCount", consultationService.countByStatus(RequestStatus.RESOLVED));
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String usersPage(Model model) {
        model.addAttribute("students", userService.getUsersByRole(RoleType.ROLE_STUDENT));
        model.addAttribute("lecturers", userService.getUsersByRole(RoleType.ROLE_LECTURER));
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            userService.updateUserStatus(id, !Boolean.TRUE.equals(user.getIsActive()));
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái tài khoản " + user.getUsername() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/departments")
    public String departmentsPage(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("newDepartment", new Department());
        model.addAttribute("newField", new ConsultationField());
        return "admin/departments";
    }

    @PostMapping("/departments")
    public String createDepartment(@ModelAttribute("newDepartment") Department department, RedirectAttributes redirectAttributes) {
        try {
            departmentService.createDepartment(department);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm Khoa/Phòng ban thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/fields")
    public String createField(@RequestParam("name") String name,
                              @RequestParam("description") String description,
                              @RequestParam("departmentId") Long departmentId,
                              RedirectAttributes redirectAttributes) {
        try {
            Department dept = departmentService.getDepartmentById(departmentId);
            departmentService.createField(new ConsultationField(name, description, dept));
            redirectAttributes.addFlashAttribute("successMessage", "Thêm lĩnh vực tư vấn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @GetMapping("/faqs")
    public String faqsPage(Model model) {
        model.addAttribute("faqs", faqService.getAllFaqsForAdmin());
        model.addAttribute("fields", departmentService.getAllFields());
        model.addAttribute("newFaq", new Faq());
        return "admin/faqs";
    }

    @PostMapping("/faqs")
    public String createFaq(@RequestParam("question") String question,
                            @RequestParam("answer") String answer,
                            @RequestParam("fieldId") Long fieldId,
                            RedirectAttributes redirectAttributes) {
        try {
            ConsultationField field = departmentService.getFieldById(fieldId);
            faqService.createFaq(new Faq(question, answer, field));
            redirectAttributes.addFlashAttribute("successMessage", "Thêm câu hỏi thường gặp FAQ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    @PostMapping("/faqs/{id}/delete")
    public String deleteFaq(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            faqService.deleteFaq(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa câu hỏi FAQ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }
}
