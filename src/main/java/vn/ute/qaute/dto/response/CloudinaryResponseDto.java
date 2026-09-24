package vn.ute.qaute.dto.response;

public class CloudinaryResponseDto {

    private String publicId;
    private String url;
    private String format;
    private Long bytes;
    private String resourceType;

    public CloudinaryResponseDto() {
    }

    public CloudinaryResponseDto(String publicId, String url, String format, Long bytes, String resourceType) {
        this.publicId = publicId;
        this.url = url;
        this.format = format;
        this.bytes = bytes;
        this.resourceType = resourceType;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
