package vn.ute.qaute.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AnswerRequestDto {

    @NotBlank(message = "Nội dung câu trả lời không được để trống")
    private String answerContent;

    private String answerFiles;

    public AnswerRequestDto() {
    }

    public AnswerRequestDto(String answerContent, String answerFiles) {
        this.answerContent = answerContent;
        this.answerFiles = answerFiles;
    }

    public String getAnswerContent() {
        return answerContent;
    }

    public void setAnswerContent(String answerContent) {
        this.answerContent = answerContent;
    }

    public String getAnswerFiles() {
        return answerFiles;
    }

    public void setAnswerFiles(String answerFiles) {
        this.answerFiles = answerFiles;
    }
}
