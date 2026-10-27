package vn.ute.qaute.service.impl;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.dto.request.LoginRequest;
import vn.ute.qaute.dto.request.RegisterRequest;
import vn.ute.qaute.dto.request.ResetPasswordRequest;
import vn.ute.qaute.dto.response.AuthResponse;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RoleType;
import vn.ute.qaute.exception.BadRequestException;
import vn.ute.qaute.repository.UserRepository;
import vn.ute.qaute.security.jwt.JwtUtils;
import vn.ute.qaute.security.service.UserDetailsImpl;
import vn.ute.qaute.service.AuthService;
import vn.ute.qaute.service.OtpService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils,
                           OtpService otpService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.otpService = otpService;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new AuthResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getFullName(),
                userDetails.getEmail(),
                userDetails.getAvatarUrl(),
                roles
        );
    }

    @Override
    @Transactional
    public void registerStudent(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã được sử dụng");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được đăng ký");
        }

        // Xác thực mã OTP trước khi tạo tài khoản
        if (request.getOtp() == null || !otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn. Vui lòng thử lại!");
        }

        User student = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getStudentCode(),
                RoleType.ROLE_STUDENT,
                null
        );
        student.setAvatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");

        userRepository.save(student);
    }

    @Override
    public void sendRegisterOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email không được để trống");
        }
        if (userRepository.existsByEmail(email.trim())) {
            throw new BadRequestException("Email này đã được sử dụng bởi một tài khoản khác");
        }
        otpService.generateAndSendOtp(email.trim(), "Đăng ký tài khoản sinh viên");
    }

    @Override
    public void sendForgotPasswordOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email không được để trống");
        }
        if (!userRepository.existsByEmail(email.trim())) {
            throw new BadRequestException("Email này chưa được đăng ký trong hệ thống");
        }
        otpService.generateAndSendOtp(email.trim(), "Đặt lại mật khẩu");
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new BadRequestException("Email không tồn tại trong hệ thống"));

        if (!otpService.verifyOtp(request.getEmail().trim(), request.getOtp())) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public Cookie createAuthCookie(String token, boolean rememberMe) {
        Cookie cookie = new Cookie(jwtUtils.getCookieName(), token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(rememberMe ? 7 * 24 * 60 * 60 : 24 * 60 * 60); // 7 ngày hoặc 1 ngày
        return cookie;
    }

    @Override
    public Cookie createLogoutCookie() {
        Cookie cookie = new Cookie(jwtUtils.getCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }
}
