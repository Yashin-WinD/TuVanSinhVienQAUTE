package vn.ute.qaute.controller.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.ute.qaute.dto.request.AnswerRequestDto;
import vn.ute.qaute.dto.request.CreateConsultationDto;
import vn.ute.qaute.dto.request.FeedbackDto;
import vn.ute.qaute.dto.response.ApiResponse;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.service.ConsultationService;
import vn.ute.qaute.service.UserService;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationApiController {

    private final ConsultationService consultationService;
    private final UserService userService;

    @Autowired
    public ConsultationApiController(ConsultationService consultationService, UserService userService) {
        this.consultationService = consultationService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ConsultationRequest>> createRequest(@Valid @RequestBody CreateConsultationDto dto) {
        User student = userService.getCurrentUser();
        ConsultationRequest created = consultationService.createRequest(dto, student);
        return ResponseEntity.ok(ApiResponse.ok("Gửi yêu cầu tư vấn thành công", created));
    }

    @PostMapping("/{id}/answer")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<ApiResponse<Void>> answerRequest(@PathVariable("id") Long id,
                                                           @Valid @RequestBody AnswerRequestDto dto) {
        User lecturer = userService.getCurrentUser();
        consultationService.answerRequest(id, dto, lecturer);
        return ResponseEntity.ok(ApiResponse.ok("Giải đáp yêu cầu thành công"));
    }

    @PostMapping("/{id}/rate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> rateRequest(@PathVariable("id") Long id,
                                                         @Valid @RequestBody FeedbackDto dto) {
        User student = userService.getCurrentUser();
        consultationService.rateConsultation(id, dto, student);
        return ResponseEntity.ok(ApiResponse.ok("Cảm ơn bạn đã đánh giá chất lượng tư vấn!"));
    }
}
