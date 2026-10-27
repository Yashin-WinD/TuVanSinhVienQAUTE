package vn.ute.qaute.service;

public interface MailService {
    void sendOtpEmail(String toEmail, String otp, String purpose);
    void sendNotificationEmail(String toEmail, String subject, String title, String message, String actionUrl);
}
