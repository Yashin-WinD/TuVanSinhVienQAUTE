package vn.ute.qaute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RoleType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRole(RoleType role);
    List<User> findByDepartmentIdAndRole(Long departmentId, RoleType role);
    long countByRole(RoleType role);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:departmentId IS NULL OR u.department.id = :departmentId) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.userCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY u.id DESC")
    List<User> filterUsers(@Param("role") RoleType role,
                           @Param("departmentId") Long departmentId,
                           @Param("isActive") Boolean isActive,
                           @Param("keyword") String keyword);
}
