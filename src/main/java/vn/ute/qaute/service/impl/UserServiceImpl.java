package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RoleType;
import vn.ute.qaute.exception.BadRequestException;
import vn.ute.qaute.exception.ResourceNotFoundException;
import vn.ute.qaute.repository.UserRepository;
import vn.ute.qaute.service.UserService;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với tên đăng nhập: " + username));
    }

    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @Override
    public List<User> getUsersByRole(RoleType role) {
        return userRepository.findByRole(role);
    }

    @Override
    public List<User> getLecturersByDepartment(Long departmentId) {
        return userRepository.findByDepartmentIdAndRole(departmentId, RoleType.ROLE_LECTURER);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, String fullName, String phone, String avatarUrl) {
        User user = getUserById(userId);
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BadRequestException("Mật khẩu mới phải có từ 6 ký tự trở lên!");
        }
        User user = getUserById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác!");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, Boolean isActive) {
        User user = getUserById(userId);
        user.setIsActive(isActive);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if (user.getPassword() == null || user.getPassword().trim().length() < 6) {
            throw new BadRequestException("Mật khẩu phải có từ 6 ký tự trở lên!");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        return userRepository.save(user);
    }

    @Override
    public long countUsersByRole(RoleType role) {
        return userRepository.countByRole(role);
    }

    @Override
    public List<User> filterUsers(RoleType role, Long departmentId, Boolean isActive, String keyword) {
        return userRepository.filterUsers(role, departmentId, isActive, keyword);
    }
}
