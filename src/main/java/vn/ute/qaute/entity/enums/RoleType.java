package vn.ute.qaute.entity.enums;

public enum RoleType {
    ROLE_STUDENT("Sinh viên"),
    ROLE_LECTURER("Giảng viên / Cố vấn"),
    ROLE_ADMIN("Quản trị viên");

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
