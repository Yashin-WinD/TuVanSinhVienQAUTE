package vn.ute.qaute.entity.enums;

public enum DocumentCategory {
    TRAINING_REGULATION("Quy chế đào tạo"),
    SCHOLARSHIP("Học bổng & Miễn giảm học phí"),
    TRAINING_POINT("Điểm rèn luyện"),
    GRADUATION("Tốt nghiệp & Chuẩn đầu ra"),
    STUDENT_FORM("Mẫu đơn từ sinh viên");

    private final String displayName;

    DocumentCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
