package vn.ute.qaute.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.ute.qaute.service.MailService;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@qaute.ute.udn.vn}")
    private String fromEmail;

    @Autowired
    public MailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        // 1. Luôn in mã OTP ra Terminal/Console nổi bật để lập trình viên/người dùng test ngay mà không cần chờ mail
        printOtpToConsole(toEmail, otp, purpose);

        // 2. Nếu đã cấu hình JavaMailSender (SMTP) thì tiến hành gửi mail thực tế
        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail, "Cổng Tư Vấn QAUTE");
                helper.setTo(toEmail);
                helper.setSubject("[QAUTE] Mã xác thực OTP - " + purpose);

                String htmlContent = buildOtpHtmlContent(otp, purpose);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Đã gửi email OTP thành công tới: {}", toEmail);
            } catch (Exception e) {
                log.warn("Không thể gửi email thực tế qua SMTP (Bạn có thể dùng mã OTP đã in ở console bên dưới để test): {}", e.getMessage());
            }
        } else {
            log.info("Chưa cấu hình spring.mail.host trong application.properties. Hệ thống sử dụng chế độ Test Console.");
        }
    }

    @Override
    public void sendNotificationEmail(String toEmail, String subject, String title, String messageContent, String actionUrl) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        log.info("📧 [QAUTE NOTIFICATION] Tới: {} | Chủ đề: {} | {}", toEmail, subject, title);

        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail, "Cổng Tư Vấn QAUTE");
                helper.setTo(toEmail);
                helper.setSubject("[QAUTE] " + subject);

                String html = buildNotificationHtml(title, messageContent, actionUrl);
                helper.setText(html, true);

                mailSender.send(message);
                log.info("Đã gửi email thông báo thành công tới: {}", toEmail);
            } catch (Exception e) {
                log.warn("Không thể gửi email thông báo qua SMTP: {}", e.getMessage());
            }
        }
    }

    private String buildNotificationHtml(String title, String messageContent, String actionUrl) {
        String fullUrl = actionUrl != null ? ("http://localhost:8088" + (actionUrl.startsWith("/") ? "" : "/") + actionUrl) : "#";
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "  <h2 style='color: #2563eb; margin: 0;'>CỔNG TƯ VẤN SINH VIÊN QAUTE</h2>"
                + "  <p style='color: #64748b; font-size: 14px;'>Hệ thống hỗ trợ & tư vấn học tập</p>"
                + "</div>"
                + "<div style='background-color: #f8fafc; padding: 20px; border-radius: 6px; margin-bottom: 20px;'>"
                + "  <h3 style='color: #1e293b; margin-top: 0;'>" + title + "</h3>"
                + "  <p style='font-size: 15px; line-height: 1.6; color: #334155; white-space: pre-wrap;'>" + messageContent + "</p>"
                + "  <div style='text-align: center; margin-top: 25px;'>"
                + "    <a href='" + fullUrl + "' style='background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 10px 20px; border-radius: 6px; font-weight: bold; font-size: 14px; display: inline-block;'>Xem chi tiết trên hệ thống</a>"
                + "  </div>"
                + "</div>"
                + "<p style='font-size: 12px; color: #94a3b8; text-align: center;'>Email này được gửi tự động từ hệ thống QAUTE. Vui lòng không phản hồi trực tiếp email này.</p>"
                + "</div>";
    }

    private void printOtpToConsole(String toEmail, String otp, String purpose) {
        String border = "================================================================================";
        System.out.println("\n" + border);
        System.out.println(" 📩 [QAUTE - MÃ XÁC THỰC OTP EMAIL]");
        System.out.println("   • Người nhận : " + toEmail);
        System.out.println("   • Mục đích   : " + purpose);
        System.out.println("   • MÃ OTP     : >>> [ " + otp + " ] <<<");
        System.out.println("   • Thời hạn   : 5 phút kể từ thời điểm gửi");
        System.out.println(border + "\n");
    }

    private String buildOtpHtmlContent(String otp, String purpose) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "  <h2 style='color: #2563eb; margin: 0;'>CỔNG TƯ VẤN SINH VIÊN QAUTE</h2>"
                + "  <p style='color: #64748b; font-size: 14px;'>Hệ thống hỗ trợ & tư vấn học tập</p>"
                + "</div>"
                + "<div style='background-color: #f8fafc; padding: 20px; border-radius: 6px; text-align: center; margin-bottom: 20px;'>"
                + "  <p style='margin: 0 0 10px 0; font-size: 15px;'>Mã xác thực của bạn cho mục đích: <strong>" + purpose + "</strong></p>"
                + "  <div style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1e293b; background: #e0e7ff; padding: 12px 24px; display: inline-block; border-radius: 8px; margin: 10px 0;'>"
                + otp
                + "  </div>"
                + "  <p style='margin: 10px 0 0 0; color: #dc2626; font-size: 13px;'>Mã xác thực có hiệu lực trong vòng <strong>5 phút</strong>. Tuyệt đối không chia sẻ mã này cho người khác!</p>"
                + "</div>"
                + "<p style='font-size: 13px; color: #94a3b8; text-align: center;'>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.</p>"
                + "</div>";
    }
}
