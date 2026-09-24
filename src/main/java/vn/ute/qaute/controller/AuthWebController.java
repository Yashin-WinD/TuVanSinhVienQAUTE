package vn.ute.qaute.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.ute.qaute.dto.request.LoginRequest;
import vn.ute.qaute.dto.request.RegisterRequest;
import vn.ute.qaute.dto.response.AuthResponse;
import vn.ute.qaute.service.AuthService;
import vn.ute.qaute.service.UserService;

@Controller
public class AuthWebController {

    private final AuthService authService;
    private final UserService userService;

    @Autowired
    public AuthWebController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Model model) {
        if (logout != null) {
            Cookie logoutCookie = authService.createLogoutCookie();
            response.addCookie(logoutCookie);
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công!");
            model.addAttribute("loginRequest", new LoginRequest());
            return "auth/login";
        }

        if (userService.getCurrentUser() != null) {
            String role = userService.getCurrentUser().getRole().name();
            if ("ROLE_ADMIN".equals(role)) return "redirect:/admin/dashboard";
            if ("ROLE_LECTURER".equals(role)) return "redirect:/lecturer/dashboard";
            return "redirect:/student/dashboard";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(@ModelAttribute("loginRequest") LoginRequest request,
                              HttpServletResponse response,
                              RedirectAttributes redirectAttributes) {
        try {
            AuthResponse authResponse = authService.login(request);
            Cookie authCookie = authService.createAuthCookie(authResponse.getToken(), request.isRememberMe());
            response.addCookie(authCookie);

            if (authResponse.getRoles().contains("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (authResponse.getRoles().contains("ROLE_LECTURER")) {
                return "redirect:/lecturer/dashboard";
            } else {
                return "redirect:/student/dashboard";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đăng nhập thất bại: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (userService.getCurrentUser() != null) {
            return "redirect:/student/dashboard";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@ModelAttribute("registerRequest") RegisterRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            authService.registerStudent(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpServletRequest request, HttpServletResponse response) {
        Cookie logoutCookie = authService.createLogoutCookie();
        response.addCookie(logoutCookie);
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }
}
