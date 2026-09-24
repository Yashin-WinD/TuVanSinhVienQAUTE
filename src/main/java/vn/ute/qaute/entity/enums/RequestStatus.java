package vn.ute.qaute.entity.enums;

public enum RequestStatus {
    PENDING("Chờ tiếp nhận"),
    ASSIGNED("Đã phân công"),
    IN_PROGRESS("Đang giải quyết"),
    RESOLVED("Đã giải đáp"),
    CLOSED("Đã đóng");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
