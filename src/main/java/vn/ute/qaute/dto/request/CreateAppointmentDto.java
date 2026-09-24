package vn.ute.qaute.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class CreateAppointmentDto {

    @NotNull(message = "ID phiếu tư vấn không được để trống")
    private Long consultationId;

    @NotNull(message = "Thời gian gặp không được để trống")
    @Future(message = "Thời gian hẹn phải ở tương lai")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime meetingTime;

    @NotBlank(message = "Địa điểm hoặc Link meeting không được để trống")
    private String locationOrLink;

    private String note;

    public CreateAppointmentDto() {
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public LocalDateTime getMeetingTime() {
        return meetingTime;
    }

    public void setMeetingTime(LocalDateTime meetingTime) {
        this.meetingTime = meetingTime;
    }

    public String getLocationOrLink() {
        return locationOrLink;
    }

    public void setLocationOrLink(String locationOrLink) {
        this.locationOrLink = locationOrLink;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
