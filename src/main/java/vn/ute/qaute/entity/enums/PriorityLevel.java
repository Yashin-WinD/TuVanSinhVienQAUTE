package vn.ute.qaute.entity.enums;

public enum PriorityLevel {
    LOW("Thấp"),
    MEDIUM("Bình thường"),
    HIGH("Quan trọng"),
    URGENT("Khẩn cấp");

    private final String displayName;

    PriorityLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
