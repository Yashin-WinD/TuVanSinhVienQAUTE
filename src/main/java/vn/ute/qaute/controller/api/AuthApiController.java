package vn.ute.qaute.controller.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ute.qaute.dto.request.LoginRequest;
import vn.ute.qaute.dto.request.RegisterRequest;
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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerStudent(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng ký tài khoản sinh viên thành công! Vui lòng đăng nhập."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        Cookie logoutCookie = authService.createLogoutCookie();
        response.addCookie(logoutCookie);
        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công"));
    }
}
