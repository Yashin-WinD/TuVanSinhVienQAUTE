package vn.ute.qaute.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import vn.ute.qaute.entity.ConsultationField;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.Department;
import vn.ute.qaute.entity.Faq;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RequestStatus;
import vn.ute.qaute.entity.enums.RoleType;
import org.springframework.web.multipart.MultipartFile;
import vn.ute.qaute.dto.response.CloudinaryResponseDto;
import vn.ute.qaute.service.ChatService;
import vn.ute.qaute.service.CloudinaryService;
import vn.ute.qaute.service.ConsultationService;
import vn.ute.qaute.service.DepartmentService;
import vn.ute.qaute.service.DocumentService;
import vn.ute.qaute.service.FaqService;
import vn.ute.qaute.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ConsultationService consultationService;
    private final DepartmentService departmentService;
    private final FaqService faqService;
    private final ChatService chatService;
    private final DocumentService documentService;
    private final CloudinaryService cloudinaryService;

    @Autowired
    public AdminController(UserService userService,
                           ConsultationService consultationService,
                           DepartmentService departmentService,
                           FaqService faqService,
                           ChatService chatService,
                           DocumentService documentService,
                           CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.consultationService = consultationService;
        this.departmentService = departmentService;
        this.faqService = faqService;
        this.chatService = chatService;
        this.documentService = documentService;
        this.cloudinaryService = cloudinaryService;
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
    public String usersPage(@RequestParam(value = "departmentId", required = false) Long departmentId,
                            @RequestParam(value = "isActive", required = false) Boolean isActive,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            Model model) {
        // Sinh viên không thuộc Khoa/Phòng ban quản trị, nên không lọc sinh viên theo departmentId
        model.addAttribute("students", userService.filterUsers(RoleType.ROLE_STUDENT, null, isActive, keyword));
        model.addAttribute("lecturers", userService.filterUsers(RoleType.ROLE_LECTURER, departmentId, isActive, keyword));
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedIsActive", isActive);
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể tự khóa tài khoản của chính mình!");
                return "redirect:/admin/users";
            }
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

    @GetMapping("/chat")
    public String chatBox(@RequestParam(value = "id", required = false) Long consultationId, Model model) {
        User admin = userService.getCurrentUser();
        java.util.List<ConsultationRequest> allRequests = consultationService.getAllRequests(
                PageRequest.of(0, 50)).getContent();

        ConsultationRequest activeRequest = null;
        if (consultationId != null) {
            try {
                activeRequest = consultationService.getRequestById(consultationId);
            } catch (Exception ignored) {
            }
        }

        if (activeRequest == null && !allRequests.isEmpty()) {
            activeRequest = allRequests.get(0);
        }

        if (activeRequest != null) {
            chatService.markMessagesAsRead(activeRequest.getId(), admin.getId());
            model.addAttribute("request", activeRequest);
            model.addAttribute("messages", chatService.getChatHistory(activeRequest.getId()));
        } else {
            model.addAttribute("request", null);
            model.addAttribute("messages", java.util.Collections.emptyList());
        }

        model.addAttribute("consultations", allRequests);
        return "admin/chat";
    }

    @GetMapping("/chat/{id}")
    public String chatRoom(@PathVariable("id") Long consultationId) {
        return "redirect:/admin/chat?id=" + consultationId;
    }

    // ====================================================================
    // 1.2 & 1.3: TRUNG TÂM ĐIỀU PHỐI & QUẢN LÝ PHIẾU TƯ VẤN (ADMIN REQUESTS)
    // ====================================================================
    @GetMapping("/requests")
    public String requestsHub(@RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "departmentId", required = false) Long departmentId,
                              @RequestParam(value = "fieldId", required = false) Long fieldId,
                              @RequestParam(value = "status", required = false) RequestStatus status,
                              @RequestParam(value = "priority", required = false) vn.ute.qaute.entity.enums.PriorityLevel priority,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              Model model) {
        org.springframework.data.domain.Page<ConsultationRequest> requestsPage = consultationService.filterRequests(
                null, null, departmentId, fieldId, status, priority, keyword, PageRequest.of(page, 15));

        model.addAttribute("requestsPage", requestsPage);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("fields", departmentService.getAllFields());
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("priorities", vn.ute.qaute.entity.enums.PriorityLevel.values());
        model.addAttribute("lecturers", userService.getUsersByRole(RoleType.ROLE_LECTURER));

        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedFieldId", fieldId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("keyword", keyword);

        return "admin/requests";
    }

    @GetMapping("/requests/{id}")
    public String requestDetail(@PathVariable("id") Long id, Model model) {
        ConsultationRequest request = consultationService.getRequestById(id);
        model.addAttribute("request", request);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("fields", departmentService.getAllFields());
        model.addAttribute("priorities", vn.ute.qaute.entity.enums.PriorityLevel.values());
        model.addAttribute("lecturers", userService.getUsersByRole(RoleType.ROLE_LECTURER));
        return "admin/request-detail";
    }

    @PostMapping("/requests/{id}/assign")
    public String assignLecturer(@PathVariable("id") Long id,
                                 @RequestParam("lecturerId") Long lecturerId,
                                 RedirectAttributes redirectAttributes) {
        try {
            consultationService.assignLecturer(id, lecturerId);
            redirectAttributes.addFlashAttribute("successMessage", "Phân công giảng viên tiếp nhận phiếu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phân công: " + e.getMessage());
        }
        return "redirect:/admin/requests/" + id;
    }

    @PostMapping("/requests/{id}/priority")
    public String updatePriority(@PathVariable("id") Long id,
                                 @RequestParam("priority") vn.ute.qaute.entity.enums.PriorityLevel priority,
                                 RedirectAttributes redirectAttributes) {
        try {
            consultationService.updatePriority(id, priority);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật mức độ ưu tiên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/requests/" + id;
    }

    @PostMapping("/requests/{id}/toggle-status")
    public String toggleRequestStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            consultationService.toggleRequestStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đóng/mở lại phiếu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/requests/" + id;
    }

    @PostMapping("/requests/{id}/save-faq")
    public String saveAsFaq(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            faqService.createFaqFromRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu câu trả lời thành bài FAQ công khai thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu FAQ: " + e.getMessage());
        }
        return "redirect:/admin/requests/" + id;
    }

    // ====================================================================
    // 2.1: QUẢN LÝ CÔNG VĂN, QUY ĐỊNH & BIỂU MẪU (DOCUMENT HUB)
    // ====================================================================
    @GetMapping("/documents")
    public String documentsPage(@RequestParam(value = "category", required = false) vn.ute.qaute.entity.enums.DocumentCategory category,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                Model model) {
        model.addAttribute("documents", documentService.filterDocuments(category, keyword));
        model.addAttribute("categories", vn.ute.qaute.entity.enums.DocumentCategory.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        return "admin/documents";
    }

    @PostMapping("/documents")
    public String createDocument(@RequestParam("title") String title,
                                 @RequestParam("description") String description,
                                 @RequestParam("category") vn.ute.qaute.entity.enums.DocumentCategory category,
                                 @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getCurrentUser();
            documentService.uploadDocument(title, description, category, file, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Tải lên tài liệu văn bản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/documents";
    }

    @PostMapping("/documents/{id}/delete")
    public String deleteDocument(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            documentService.deleteDocument(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa văn bản/biểu mẫu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa: " + e.getMessage());
        }
        return "redirect:/admin/documents";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        return "admin/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.getCurrentUser();
            String avatarUrl = admin.getAvatarUrl();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                CloudinaryResponseDto uploadRes = cloudinaryService.uploadImage(avatarFile, "avatars");
                if (uploadRes != null) {
                    avatarUrl = uploadRes.getUrl();
                }
            }
            userService.updateProfile(admin.getId(), fullName, phone, avatarUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ quản trị viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có từ 6 ký tự trở lên!");
            return "redirect:/admin/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "redirect:/admin/profile";
        }
        try {
            User admin = userService.getCurrentUser();
            userService.changePassword(admin.getId(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/profile";
    }
}
