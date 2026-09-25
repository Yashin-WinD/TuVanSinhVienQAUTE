package vn.ute.qaute.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.ute.qaute.entity.*;
import vn.ute.qaute.entity.enums.PriorityLevel;
import vn.ute.qaute.entity.enums.RequestStatus;
import vn.ute.qaute.entity.enums.RoleType;
import vn.ute.qaute.repository.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ConsultationFieldRepository fieldRepository;
    private final FaqRepository faqRepository;
    private final ConsultationRequestRepository requestRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           ConsultationFieldRepository fieldRepository,
                           FaqRepository faqRepository,
                           ConsultationRequestRepository requestRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.fieldRepository = fieldRepository;
        this.faqRepository = faqRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Tạo phòng ban nếu chưa có
        if (departmentRepository.count() == 0) {
            logger.info("Khởi tạo dữ liệu Khoa và Phòng ban...");
            Department d1 = departmentRepository.save(new Department("CTSV", "Phòng Công tác sinh viên", "Giải quyết chế độ chính sách, học bổng, rèn luyện", "ctsv@hcmute.edu.vn", "028.38968641"));
            Department d2 = departmentRepository.save(new Department("DAOTAO", "Phòng Đào tạo", "Tư vấn đăng ký môn học, thời khóa biểu, xét tốt nghiệp", "daotao@hcmute.edu.vn", "028.38968642"));
            Department d3 = departmentRepository.save(new Department("FIT", "Khoa Công nghệ thông tin", "Tư vấn chuyên ngành CNTT, đồ án môn học", "fit@hcmute.edu.vn", "028.38968643"));
            Department d4 = departmentRepository.save(new Department("FME", "Khoa Cơ khí Chế tạo máy", "Tư vấn chuyên ngành Cơ khí, xưởng thực hành", "fme@hcmute.edu.vn", "028.38968644"));

            // 2. Tạo lĩnh vực tư vấn
            fieldRepository.save(new ConsultationField("Chế độ chính sách & Học bổng", "Tư vấn miễn giảm học phí, học bổng", d1));
            ConsultationField fDrl = fieldRepository.save(new ConsultationField("Điểm rèn luyện & Khen thưởng", "Khiếu nại điểm rèn luyện, chứng nhận ngoại khóa", d1));
            fieldRepository.save(new ConsultationField("Đăng ký học phần & TKB", "Xử lý trùng lịch, mở thêm lớp học phần", d2));
            ConsultationField fTotNghiep = fieldRepository.save(new ConsultationField("Xét chuẩn đầu ra & Tốt nghiệp", "Chứng chỉ ngoại ngữ, tin học, tốt nghiệp", d2));
            fieldRepository.save(new ConsultationField("Đồ án tốt nghiệp & Thực tập", "Hướng dẫn đề tài, thực tập doanh nghiệp CNTT", d3));

            // 3. Tạo FAQs mẫu
            faqRepository.save(new Faq(
                    "Làm thế nào để xin cấp lại Bảng điểm hoặc Giấy chứng nhận sinh viên?",
                    "Sinh viên có thể đăng nhập cổng đào tạo trực tuyến hoặc nộp phiếu yêu cầu tại Phòng Đào tạo (Tòa nhà Trung tâm). Thời gian cấp từ 2 - 3 ngày làm việc.",
                    fTotNghiep
            ));
            faqRepository.save(new Faq(
                    "Điều kiện để được nhận Học bổng khuyến khích học tập là gì?",
                    "Sinh viên cần tích lũy tối thiểu 15 tín chỉ trong học kỳ chính, điểm trung bình học kỳ từ 7.0 trở lên và điểm rèn luyện đạt loại Tốt (80 điểm) trở lên.",
                    fDrl
            ));
        }

        // 4. Tạo tài khoản mẫu
        if (!userRepository.existsByUsername("admin")) {
            logger.info("Khởi tạo tài khoản mẫu Admin, Giảng viên, Sinh viên...");
            Department fitDept = departmentRepository.findByCode("FIT").orElse(null);
            Department ctsvDept = departmentRepository.findByCode("CTSV").orElse(null);

            User admin = new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "Quản trị viên QAUTE",
                    "admin@hcmute.edu.vn",
                    "0901234567",
                    "ADMIN001",
                    RoleType.ROLE_ADMIN,
                    null
            );
            admin.setAvatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150");
            userRepository.save(admin);

            User gv = new User(
                    "giangvien",
                    passwordEncoder.encode("gv123456"),
                    "TS. Nguyễn Văn Giảng Viên",
                    "gv_fit@hcmute.edu.vn",
                    "0912345678",
                    "GV001",
                    RoleType.ROLE_LECTURER,
                    fitDept
            );
            gv.setAvatarUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150");
            userRepository.save(gv);

            User gvCtsv = new User(
                    "giangvien_ctsv",
                    passwordEncoder.encode("gv123456"),
                    "ThS. Trần Thị Cố Vấn",
                    "gv_ctsv@hcmute.edu.vn",
                    "0923456789",
                    "GV002",
                    RoleType.ROLE_LECTURER,
                    ctsvDept
            );
            gvCtsv.setAvatarUrl("https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150");
            userRepository.save(gvCtsv);

            User sv = new User(
                    "sinhvien",
                    passwordEncoder.encode("sv123456"),
                    "Lê Văn Sinh Viên",
                    "21110001@student.hcmute.edu.vn",
                    "0934567890",
                    "21110001",
                    RoleType.ROLE_STUDENT,
                    null
            );
            sv.setAvatarUrl("https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150");
            User savedSv = userRepository.save(sv);

            // Tạo thử 1 phiếu tư vấn mẫu
            if (fieldRepository.count() > 0) {
                ConsultationField sampleField = fieldRepository.findAll().get(0);
                ConsultationRequest sampleReq = new ConsultationRequest(
                        "Hỏi về thủ tục phúc khảo điểm rèn luyện",
                        "Em chào thầy cô, học kỳ vừa qua em có tham gia chiến dịch tình nguyện Mùa hè xanh nhưng chưa thấy được cộng điểm rèn luyện. Nhờ thầy cô kiểm tra giúp em ạ.",
                        null,
                        savedSv,
                        sampleField,
                        PriorityLevel.MEDIUM
                );
                sampleReq.setLecturer(gvCtsv);
                sampleReq.setStatus(RequestStatus.ASSIGNED);
                requestRepository.save(sampleReq);
            }

            logger.info("Dữ liệu ban đầu đã được khởi tạo hoàn tất!");
        }
    }
}
