package vn.ute.qaute.service;

import jakarta.servlet.http.Cookie;
import vn.ute.qaute.dto.request.LoginRequest;
import vn.ute.qaute.dto.request.RegisterRequest;
import vn.ute.qaute.dto.request.ResetPasswordRequest;
import vn.ute.qaute.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void registerStudent(RegisterRequest request);
    Cookie createAuthCookie(String token, boolean rememberMe);
    Cookie createLogoutCookie();

    // Các hàm xử lý OTP cho Đăng ký & Quên mật khẩu
    void sendRegisterOtp(String email);
    void sendForgotPasswordOtp(String email);
    void resetPasswordWithOtp(ResetPasswordRequest request);
}
