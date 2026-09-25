-- ====================================================================
-- HỆ THỐNG TƯ VẤN SINH VIÊN QAUTE (UTE CONSULTING PORTAL)
-- File tạo Database, các bảng và nạp dữ liệu mẫu ban đầu
-- ====================================================================

-- 1. Tạo Database
CREATE DATABASE IF NOT EXISTS `qaute_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `qaute_db`;

-- 2. Xóa các bảng cũ nếu tồn tại (theo thứ tự khóa ngoại)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `faqs`;
DROP TABLE IF EXISTS `appointments`;
DROP TABLE IF EXISTS `chat_messages`;
DROP TABLE IF EXISTS `consultation_requests`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `consultation_fields`;
DROP TABLE IF EXISTS `departments`;
SET FOREIGN_KEY_CHECKS = 1;

-- --------------------------------------------------------
-- 3. Tạo bảng `departments` (Khoa / Phòng ban)
-- --------------------------------------------------------
CREATE TABLE `departments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `email` VARCHAR(150),
    `hotline` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 4. Tạo bảng `consultation_fields` (Lĩnh vực tư vấn theo khoa)
-- --------------------------------------------------------
CREATE TABLE `consultation_fields` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `department_id` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_field_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 5. Tạo bảng `users` (Tài khoản người dùng: Sinh viên, Giảng viên, Admin)
-- --------------------------------------------------------
CREATE TABLE `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(150) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `phone` VARCHAR(20),
    `avatar_url` VARCHAR(500),
    `user_code` VARCHAR(50),
    `role` VARCHAR(50) NOT NULL, -- ROLE_STUDENT, ROLE_LECTURER, ROLE_ADMIN
    `department_id` BIGINT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 6. Tạo bảng `consultation_requests` (Phiếu câu hỏi tư vấn)
-- --------------------------------------------------------
CREATE TABLE `consultation_requests` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `content` LONGTEXT NOT NULL,
    `attachment_urls` TEXT,
    `student_id` BIGINT NOT NULL,
    `lecturer_id` BIGINT NULL,
    `field_id` BIGINT NOT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED
    `priority` VARCHAR(50) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    `answer_content` LONGTEXT NULL,
    `answer_files` TEXT NULL,
    `rating` INT NULL, -- 1 đến 5 sao
    `feedback` TEXT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `resolved_at` DATETIME NULL,
    CONSTRAINT `fk_request_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_request_lecturer` FOREIGN KEY (`lecturer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_request_field` FOREIGN KEY (`field_id`) REFERENCES `consultation_fields` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 7. Tạo bảng `chat_messages` (Tin nhắn trao đổi trực tiếp)
-- --------------------------------------------------------
CREATE TABLE `chat_messages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `consultation_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` TEXT,
    `media_url` VARCHAR(500),
    `message_type` VARCHAR(50) NOT NULL DEFAULT 'TEXT', -- TEXT, IMAGE, FILE, SYSTEM
    `is_read` BOOLEAN DEFAULT FALSE,
    `sent_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_chat_consultation` FOREIGN KEY (`consultation_id`) REFERENCES `consultation_requests` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 8. Tạo bảng `appointments` (Lịch hẹn tư vấn)
-- --------------------------------------------------------
CREATE TABLE `appointments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `consultation_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `lecturer_id` BIGINT NOT NULL,
    `meeting_time` DATETIME NOT NULL,
    `location_or_link` VARCHAR(255),
    `note` TEXT,
    `status` VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, COMPLETED, CANCELLED
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_appointment_consultation` FOREIGN KEY (`consultation_id`) REFERENCES `consultation_requests` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_appointment_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_appointment_lecturer` FOREIGN KEY (`lecturer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 9. Tạo bảng `faqs` (Câu hỏi thường gặp)
-- --------------------------------------------------------
CREATE TABLE `faqs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `question` VARCHAR(500) NOT NULL,
    `answer` LONGTEXT NOT NULL,
    `field_id` BIGINT NULL,
    `view_count` INT DEFAULT 0,
    `is_published` BOOLEAN DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_faq_field` FOREIGN KEY (`field_id`) REFERENCES `consultation_fields` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 10. Tạo bảng `notifications` (Thông báo hệ thống)
-- --------------------------------------------------------
CREATE TABLE `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recipient_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `target_url` VARCHAR(255),
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- NẠP DỮ LIỆU MẪU BAN ĐẦU (SEED DATA)
-- Mật khẩu mặc định:
-- admin123     -> $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- gv123456     -> $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- sv123456     -> $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- (Chú ý: DataInitializer trong Spring Boot cũng sẽ tự động đồng bộ tài khoản nếu chạy trực tiếp)
-- ========================================================

-- 1. Thêm Khoa / Phòng ban
INSERT INTO `departments` (`id`, `code`, `name`, `description`, `email`, `hotline`) VALUES
(1, 'CTSV', 'Phòng Công tác sinh viên', 'Giải quyết chế độ chính sách, học bổng, rèn luyện, bảo hiểm y tế', 'ctsv@hcmute.edu.vn', '028.38968641'),
(2, 'DAOTAO', 'Phòng Đào tạo', 'Tư vấn đăng ký môn học, thời khóa biểu, xét tốt nghiệp, cấp bảng điểm', 'daotao@hcmute.edu.vn', '028.38968642'),
(3, 'FIT', 'Khoa Công nghệ thông tin', 'Tư vấn chuyên ngành CNTT, đồ án môn học, định hướng việc làm', 'fit@hcmute.edu.vn', '028.38968643'),
(4, 'FME', 'Khoa Cơ khí Chế tạo máy', 'Tư vấn chuyên ngành Cơ khí, xưởng thực hành, nghiên cứu khoa học', 'fme@hcmute.edu.vn', '028.38968644');

-- 2. Thêm Lĩnh vực tư vấn
INSERT INTO `consultation_fields` (`id`, `name`, `description`, `department_id`) VALUES
(1, 'Chế độ chính sách & Học bổng', 'Tư vấn miễn giảm học phí, học bổng khuyến khích học tập và tài trợ', 1),
(2, 'Đánh giá điểm rèn luyện & Khen thưởng', 'Giải đáp khiếu nại điểm rèn luyện, chứng nhận hoạt động ngoại khóa', 1),
(3, 'Đăng ký học phần & Thời khóa biểu', 'Xử lý trùng lịch, mở thêm lớp học phần, rút môn học', 2),
(4, 'Xét chuẩn đầu ra & Tốt nghiệp', 'Chứng chỉ ngoại ngữ, tin học, xét công nhận tốt nghiệp sớm', 2),
(5, 'Đồ án tốt nghiệp & Thực tập doanh nghiệp', 'Hướng dẫn đề tài, liên hệ doanh nghiệp thực tập ngành CNTT', 3);

-- 3. Thêm Tài khoản mẫu (Admin, Giảng viên, Sinh viên)
-- Mật khẩu hash bên dưới tương ứng với 'password123'
INSERT INTO `users` (`id`, `username`, `password`, `full_name`, `email`, `phone`, `avatar_url`, `user_code`, `role`, `department_id`, `is_active`) VALUES
(1, 'admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Quản trị viên QAUTE', 'admin@hcmute.edu.vn', '0901234567', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', 'ADMIN001', 'ROLE_ADMIN', NULL, TRUE),
(2, 'giangvien', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'TS. Nguyễn Văn Giảng Viên', 'gv_fit@hcmute.edu.vn', '0912345678', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', 'GV001', 'ROLE_LECTURER', 3, TRUE),
(3, 'giangvien_ctsv', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'ThS. Trần Thị Cố Vấn', 'gv_ctsv@hcmute.edu.vn', '0923456789', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150', 'GV002', 'ROLE_LECTURER', 1, TRUE),
(4, 'sinhvien', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Lê Văn Sinh Viên', '21110001@student.hcmute.edu.vn', '0934567890', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150', '21110001', 'ROLE_STUDENT', NULL, TRUE);

-- 4. Thêm Câu hỏi FAQ mẫu
INSERT INTO `faqs` (`id`, `question`, `answer`, `field_id`, `view_count`, `is_published`) VALUES
(1, 'Làm thế nào để xin cấp lại Bảng điểm hoặc Giấy chứng nhận sinh viên?', 'Sinh viên có thể đăng nhập cổng đào tạo trực tuyến hoặc nộp phiếu yêu cầu tại Phòng Đào tạo (Tòa nhà Trung tâm). Thời gian cấp từ 2 - 3 ngày làm việc.', 4, 150, TRUE),
(2, 'Điều kiện để được nhận Học bổng khuyến khích học tập là gì?', 'Sinh viên cần tích lũy tối thiểu 15 tín chỉ trong học kỳ chính, điểm trung bình học kỳ từ 7.0 trở lên và điểm rèn luyện đạt loại Tốt (80 điểm) trở lên, không có môn nào bị điểm F.', 1, 320, TRUE),
(3, 'Nếu lỡ quên đăng ký môn học trong đợt chính thì phải làm sao?', 'Bạn cần theo dõi thông báo đợt đăng ký bổ sung trên trang Đào tạo hoặc gửi phiếu tư vấn trực tiếp đến chuyên viên phụ trách qua hệ thống QAUTE này.', 3, 210, TRUE);

-- 5. Thêm Phiếu tư vấn mẫu
INSERT INTO `consultation_requests` (`id`, `title`, `content`, `attachment_urls`, `student_id`, `lecturer_id`, `field_id`, `status`, `priority`, `created_at`) VALUES
(1, 'Hỏi về thủ tục phúc khảo điểm rèn luyện học kỳ 2', 'Em chào thầy cô, học kỳ vừa qua em có tham gia chiến dịch tình nguyện Mùa hè xanh nhưng chưa thấy được cộng 10 điểm vào mục rèn luyện cộng đồng. Nhờ thầy cô kiểm tra giúp em ạ.', NULL, 4, 3, 2, 'ASSIGNED', 'MEDIUM', NOW() - INTERVAL 1 DAY);
