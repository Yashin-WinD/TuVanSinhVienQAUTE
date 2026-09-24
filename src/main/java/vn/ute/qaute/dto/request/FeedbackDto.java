package vn.ute.qaute.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class FeedbackDto {

    @NotNull(message = "Vui lòng chọn số sao đánh giá")
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    private Integer rating;

    private String feedback;

    public FeedbackDto() {
    }

    public FeedbackDto(Integer rating, String feedback) {
        this.rating = rating;
        this.feedback = feedback;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
