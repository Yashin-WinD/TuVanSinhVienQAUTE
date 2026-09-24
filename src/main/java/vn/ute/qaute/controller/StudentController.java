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

    @Autowired
    public StudentController(UserService userService,
                             ConsultationService consultationService,
                             DepartmentService departmentService,
                             ChatService chatService,
                             CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.consultationService = consultationService;
        this.departmentService = departmentService;
        this.chatService = chatService;
        this.cloudinaryService = cloudinaryService;
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
                                      RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getCurrentUser();

            // Nếu có đính kèm file ảnh minh chứng thì upload lên Cloudinary
            if (evidenceFile != null && !evidenceFile.isEmpty()) {
                CloudinaryResponseDto uploadRes = cloudinaryService.uploadImage(evidenceFile, "evidence");
                if (uploadRes != null) {
                    dto.setAttachmentUrls(uploadRes.getUrl());
                }
            }

            ConsultationRequest created = consultationService.createRequest(dto, student);
            redirectAttributes.addFlashAttribute("successMessage", "Gửi câu hỏi tư vấn thành công! Mã phiếu: #" + created.getId());
            return "redirect:/student/requests/" + created.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi yêu cầu: " + e.getMessage());
            return "redirect:/student/requests/create";
        }
    }

    @GetMapping("/requests")
    public String requestList(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        User student = userService.getCurrentUser();
        model.addAttribute("requestsPage", consultationService.getRequestsByStudent(student.getId(), PageRequest.of(page, 10)));
        return "student/request-list";
    }

    @GetMapping("/requests/{id}")
    public String requestDetail(@PathVariable("id") Long id, Model model) {
        ConsultationRequest req = consultationService.getRequestById(id);
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

    @GetMapping("/chat/{id}")
    public String chatRoom(@PathVariable("id") Long consultationId, Model model) {
        User student = userService.getCurrentUser();
        ConsultationRequest req = consultationService.getRequestById(consultationId);

        // Đánh dấu đã đọc tin nhắn
        chatService.markMessagesAsRead(consultationId, student.getId());

        model.addAttribute("request", req);
        model.addAttribute("messages", chatService.getChatHistory(consultationId));
        return "student/chat";
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
