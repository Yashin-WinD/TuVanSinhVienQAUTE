package vn.ute.qaute.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.ute.qaute.dto.request.CreateConsultationDto;
import vn.ute.qaute.dto.request.FeedbackDto;
import vn.ute.qaute.dto.response.CloudinaryResponseDto;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RequestStatus;
import vn.ute.qaute.service.*;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final UserService userService;
    private final ConsultationService consultationService;
    private final DepartmentService departmentService;
    private final ChatService chatService;
    private final CloudinaryService cloudinaryService;
    private final AppointmentService appointmentService;

    @Autowired
    public StudentController(UserService userService,
                             ConsultationService consultationService,
                             DepartmentService departmentService,
                             ChatService chatService,
                             CloudinaryService cloudinaryService,
                             AppointmentService appointmentService) {
        this.userService = userService;
        this.consultationService = consultationService;
        this.departmentService = departmentService;
        this.chatService = chatService;
        this.cloudinaryService = cloudinaryService;
        this.appointmentService = appointmentService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("currentUser", currentUser);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User student = userService.getCurrentUser();
        model.addAttribute("totalRequests", consultationService.countByStudent(student.getId()));
        model.addAttribute("recentRequests", consultationService.getRequestsByStudent(student.getId(), PageRequest.of(0, 5)).getContent());
        return "student/dashboard";
    }

    @GetMapping("/requests/create")
    public String createRequestForm(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("fields", departmentService.getAllFields());
        model.addAttribute("createDto", new CreateConsultationDto());
        return "student/create-request";
    }

    @PostMapping("/requests/create")
    public String handleCreateRequest(@ModelAttribute("createDto") CreateConsultationDto dto,
                                      @RequestParam(value = "evidenceFile", required = false) MultipartFile evidenceFile,
                                      @RequestParam(value = "redirectChat", required = false) Boolean redirectChat,
                                      RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getCurrentUser();

            // Nếu có đính kèm file minh chứng (ảnh, PDF, tài liệu) thì lưu tức thì và đồng bộ Cloudinary
            if (evidenceFile != null && !evidenceFile.isEmpty()) {
                CloudinaryResponseDto uploadRes = cloudinaryService.uploadFile(evidenceFile, "evidence");
                if (uploadRes != null) {
                    dto.setAttachmentUrls(uploadRes.getUrl());
                }
            }

            ConsultationRequest created = consultationService.createRequest(dto, student);
            redirectAttributes.addFlashAttribute("successMessage", "Gửi câu hỏi tư vấn thành công! Mã phiếu: #" + created.getId());
            if (Boolean.TRUE.equals(redirectChat)) {
                return "redirect:/student/chat?id=" + created.getId();
            }
            return "redirect:/student/requests/" + created.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi yêu cầu: " + e.getMessage());
            if (Boolean.TRUE.equals(redirectChat)) {
                return "redirect:/student/chat";
            }
            return "redirect:/student/requests/create";
        }
    }

    @GetMapping("/requests")
    public String requestList(@RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "status", required = false) RequestStatus status,
                              @RequestParam(value = "fieldId", required = false) Long fieldId,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              Model model) {
        User student = userService.getCurrentUser();
        org.springframework.data.domain.Page<ConsultationRequest> requestsPage = consultationService.filterRequests(
                student.getId(), null, null, fieldId, status, null, keyword, PageRequest.of(page, 10));
        model.addAttribute("requestsPage", requestsPage);
        model.addAttribute("fields", departmentService.getAllFields());
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedFieldId", fieldId);
        model.addAttribute("keyword", keyword);
        return "student/request-list";
    }

    @GetMapping("/appointments")
    public String appointmentsPage(@RequestParam(value = "status", required = false) vn.ute.qaute.entity.enums.AppointmentStatus status, Model model) {
        User student = userService.getCurrentUser();
        java.util.List<vn.ute.qaute.entity.Appointment> appointments = appointmentService.getAppointmentsByStudentAndStatus(student.getId(), status);
        model.addAttribute("appointments", appointments);
        model.addAttribute("statuses", vn.ute.qaute.entity.enums.AppointmentStatus.values());
        model.addAttribute("selectedStatus", status);
        return "student/appointments";
    }

    @PostMapping("/appointments/{id}/confirm")
    public String confirmAppointment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getCurrentUser();
            appointmentService.confirmAppointment(id, student);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận lịch hẹn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/student/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable("id") Long id,
                                    @RequestParam("reason") String reason,
                                    RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getCurrentUser();
            appointmentService.cancelAppointment(id, reason, student);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu hủy/xin dời lịch hẹn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/student/appointments";
    }

    @GetMapping("/requests/{id}")
    public String requestDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        User student = userService.getCurrentUser();
        ConsultationRequest req = consultationService.getRequestById(id);
        if (req.getStudent() == null || !req.getStudent().getId().equals(student.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem phiếu tư vấn này!");
            return "redirect:/student/requests";
        }
        model.addAttribute("request", req);
        model.addAttribute("feedbackDto", new FeedbackDto());
        return "student/request-detail";
    }

    @PostMapping("/requests/{id}/rate")
    public String handleRateRequest(@PathVariable("id") Long id,
                                    @ModelAttribute("feedbackDto") FeedbackDto dto,
                                    RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getCurrentUser();
            consultationService.rateConsultation(id, dto, student);
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá chất lượng tư vấn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/student/requests/" + id;
    }

    @GetMapping("/chat")
    public String chatBox(@RequestParam(value = "id", required = false) Long consultationId, Model model) {
        User student = userService.getCurrentUser();
        java.util.List<ConsultationRequest> consultations = consultationService.getRequestsByStudent(
                student.getId(), PageRequest.of(0, 50)).getContent();

        ConsultationRequest activeRequest = null;
        if (consultationId != null) {
            try {
                ConsultationRequest req = consultationService.getRequestById(consultationId);
                if (req.getStudent().getId().equals(student.getId())) {
                    activeRequest = req;
                }
            } catch (Exception ignored) {
            }
        }

        if (activeRequest == null && !consultations.isEmpty()) {
            activeRequest = consultations.get(0);
        }

        if (activeRequest != null) {
            chatService.markMessagesAsRead(activeRequest.getId(), student.getId());
            model.addAttribute("request", activeRequest);
            model.addAttribute("messages", chatService.getChatHistory(activeRequest.getId()));
        } else {
            model.addAttribute("request", null);
            model.addAttribute("messages", java.util.Collections.emptyList());
        }

        model.addAttribute("consultations", consultations);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("fields", departmentService.getAllFields());
        model.addAttribute("newConsultationDto", new CreateConsultationDto());
        return "student/chat";
    }

    @GetMapping("/chat/{id}")
    public String chatRoom(@PathVariable("id") Long consultationId) {
        return "redirect:/student/chat?id=" + consultationId;
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getCurrentUser();
            String avatarUrl = student.getAvatarUrl();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                CloudinaryResponseDto uploadRes = cloudinaryService.uploadImage(avatarFile, "avatars");
                if (uploadRes != null) {
                    avatarUrl = uploadRes.getUrl();
                }
            }
            userService.updateProfile(student.getId(), fullName, phone, avatarUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/student/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có từ 6 ký tự trở lên!");
            return "redirect:/student/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "redirect:/student/profile";
        }
        try {
            User student = userService.getCurrentUser();
            userService.changePassword(student.getId(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/student/profile";
    }
}
