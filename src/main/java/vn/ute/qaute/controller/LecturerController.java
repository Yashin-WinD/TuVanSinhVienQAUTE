package vn.ute.qaute.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.ute.qaute.dto.request.AnswerRequestDto;
import vn.ute.qaute.dto.request.CreateAppointmentDto;
import vn.ute.qaute.dto.response.CloudinaryResponseDto;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RequestStatus;
import vn.ute.qaute.service.*;

@Controller
@RequestMapping("/lecturer")
public class LecturerController {

    private final UserService userService;
    private final ConsultationService consultationService;
    private final ChatService chatService;
    private final AppointmentService appointmentService;
    private final CloudinaryService cloudinaryService;

    @Autowired
    public LecturerController(UserService userService,
                              ConsultationService consultationService,
                              ChatService chatService,
                              AppointmentService appointmentService,
                              CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.consultationService = consultationService;
        this.chatService = chatService;
        this.appointmentService = appointmentService;
        this.cloudinaryService = cloudinaryService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("currentUser", currentUser);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User lecturer = userService.getCurrentUser();
        model.addAttribute("assignedCount", consultationService.countByLecturerAndStatus(lecturer.getId(), RequestStatus.ASSIGNED));
        model.addAttribute("resolvedCount", consultationService.countByLecturerAndStatus(lecturer.getId(), RequestStatus.RESOLVED));

        // Các câu hỏi được gán gần đây
        model.addAttribute("recentRequests", consultationService.getRequestsByLecturer(lecturer.getId(), PageRequest.of(0, 5)).getContent());

        // Nếu giảng viên có khoa, hiển thị thêm các câu hỏi chờ tiếp nhận thuộc khoa
        if (lecturer.getDepartment() != null) {
            model.addAttribute("pendingDeptRequests",
                    consultationService.getPendingRequestsByDepartment(lecturer.getDepartment().getId(), PageRequest.of(0, 5)).getContent());
        }

        return "lecturer/dashboard";
    }

    @GetMapping("/requests/pending")
    public String pendingRequests(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        User lecturer = userService.getCurrentUser();
        if (lecturer.getDepartment() != null) {
            model.addAttribute("pendingPage",
                    consultationService.getPendingRequestsByDepartment(lecturer.getDepartment().getId(), PageRequest.of(page, 10)));
        }
        return "lecturer/pending-requests";
    }

    @PostMapping("/requests/{id}/claim")
    public String claimRequest(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User lecturer = userService.getCurrentUser();
            consultationService.assignLecturer(id, lecturer.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã tiếp nhận phiếu tư vấn #" + id);
            return "redirect:/lecturer/requests/" + id + "/answer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tiếp nhận: " + e.getMessage());
            return "redirect:/lecturer/requests/pending";
        }
    }

    @GetMapping("/requests/{id}/answer")
    public String answerForm(@PathVariable("id") Long id, Model model) {
        ConsultationRequest req = consultationService.getRequestById(id);
        model.addAttribute("request", req);
        model.addAttribute("answerDto", new AnswerRequestDto(req.getAnswerContent(), req.getAnswerFiles()));
        return "lecturer/answer-request";
    }

    @PostMapping("/requests/{id}/answer")
    public String handleAnswer(@PathVariable("id") Long id,
                               @ModelAttribute("answerDto") AnswerRequestDto dto,
                               @RequestParam(value = "answerFile", required = false) MultipartFile answerFile,
                               RedirectAttributes redirectAttributes) {
        try {
            User lecturer = userService.getCurrentUser();
            if (answerFile != null && !answerFile.isEmpty()) {
                CloudinaryResponseDto uploadRes = cloudinaryService.uploadFile(answerFile, "answers");
                if (uploadRes != null) {
                    dto.setAnswerFiles(uploadRes.getUrl());
                }
            }

            consultationService.answerRequest(id, dto, lecturer);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi câu trả lời giải đáp thành công cho sinh viên!");
            return "redirect:/lecturer/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi câu trả lời: " + e.getMessage());
            return "redirect:/lecturer/requests/" + id + "/answer";
        }
    }

    @GetMapping("/chat/{id}")
    public String chatRoom(@PathVariable("id") Long consultationId, Model model) {
        User lecturer = userService.getCurrentUser();
        ConsultationRequest req = consultationService.getRequestById(consultationId);

        // Đánh dấu đã đọc tin nhắn
        chatService.markMessagesAsRead(consultationId, lecturer.getId());

        model.addAttribute("request", req);
        model.addAttribute("messages", chatService.getChatHistory(consultationId));
        return "lecturer/chat";
    }

    @GetMapping("/schedule")
    public String schedulePage(Model model) {
        User lecturer = userService.getCurrentUser();
        model.addAttribute("appointments", appointmentService.getAppointmentsByLecturer(lecturer.getId()));
        model.addAttribute("appointmentDto", new CreateAppointmentDto());
        return "lecturer/schedule";
    }

    @PostMapping("/schedule")
    public String handleCreateSchedule(@ModelAttribute("appointmentDto") CreateAppointmentDto dto,
                                       RedirectAttributes redirectAttributes) {
        try {
            User lecturer = userService.getCurrentUser();
            appointmentService.createAppointment(dto, lecturer);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo lịch hẹn tư vấn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo lịch hẹn: " + e.getMessage());
        }
        return "redirect:/lecturer/schedule";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        return "lecturer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                RedirectAttributes redirectAttributes) {
        try {
            User lecturer = userService.getCurrentUser();
            String avatarUrl = lecturer.getAvatarUrl();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                CloudinaryResponseDto uploadRes = cloudinaryService.uploadImage(avatarFile, "avatars");
                if (uploadRes != null) {
                    avatarUrl = uploadRes.getUrl();
                }
            }
            userService.updateProfile(lecturer.getId(), fullName, phone, avatarUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin giảng viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/lecturer/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "redirect:/lecturer/profile";
        }
        try {
            User lecturer = userService.getCurrentUser();
            userService.changePassword(lecturer.getId(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lecturer/profile";
    }
}
