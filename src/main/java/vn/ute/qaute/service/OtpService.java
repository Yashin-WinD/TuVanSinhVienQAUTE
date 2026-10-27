package vn.ute.qaute.service;

public interface OtpService {
    String generateAndSendOtp(String email, String purpose);
    boolean verifyOtp(String email, String otp);
    boolean isOtpValid(String email, String otp);
}
