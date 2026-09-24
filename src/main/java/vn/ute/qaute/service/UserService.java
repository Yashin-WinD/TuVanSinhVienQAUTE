package vn.ute.qaute.service;

import vn.ute.qaute.dto.response.UserProfileDto;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RoleType;

import java.util.List;

public interface UserService {
    User getUserById(Long id);
    User getUserByUsername(String username);
    User getCurrentUser();
    List<User> getUsersByRole(RoleType role);
    List<User> getLecturersByDepartment(Long departmentId);
    void updateProfile(Long userId, String fullName, String phone, String avatarUrl);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void updateUserStatus(Long userId, Boolean isActive);
    User createUser(User user);
    long countUsersByRole(RoleType role);
}
