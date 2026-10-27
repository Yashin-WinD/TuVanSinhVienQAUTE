package vn.ute.qaute.controller.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ute.qaute.dto.request.ForgotPasswordRequest;
import vn.ute.qaute.dto.request.LoginRequest;
import vn.ute.qaute.dto.request.RegisterRequest;
import vn.ute.qaute.dto.request.ResetPasswordRequest;
import vn.ute.qaute.dto.request.SendOtpRequest;
import vn.ute.qaute.dto.response.ApiResponse;
import vn.ute.qaute.dto.response.AuthResponse;
import vn.ute.qaute.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final AuthService authService;

    @Autowired
    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        // Lưu token vào Cookie cho giao diện web
        Cookie authCookie = authService.createAuthCookie(authResponse.getToken(), request.isRememberMe());
        response.addCookie(authCookie);

        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", authResponse));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        if ("REGISTER".equalsIgnoreCase(request.getPurpose())) {
            authService.sendRegisterOtp(request.getEmail());
        } else {
            authService.sendForgotPasswordOtp(request.getEmail());
        }
        return ResponseEntity.ok(ApiResponse.ok("Mã xác thực OTP đã được gửi đến email của bạn! (Vui lòng kiểm tra email hoặc xem trực tiếp tại Terminal)"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerStudent(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng ký tài khoản sinh viên thành công! Vui lòng đăng nhập."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendForgotPasswordOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Mã OTP đặt lại mật khẩu đã được gửi đến email của bạn!"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        Cookie logoutCookie = authService.createLogoutCookie();
        response.addCookie(logoutCookie);
        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công"));
    }
}
