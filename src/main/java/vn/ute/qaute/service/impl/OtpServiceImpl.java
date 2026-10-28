package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.entity.OtpToken;
import vn.ute.qaute.repository.OtpTokenRepository;
import vn.ute.qaute.service.MailService;
import vn.ute.qaute.service.OtpService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpServiceImpl implements OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpTokenRepository otpTokenRepository;
    private final MailService mailService;

    @Autowired
    public OtpServiceImpl(OtpTokenRepository otpTokenRepository, MailService mailService) {
        this.otpTokenRepository = otpTokenRepository;
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public String generateAndSendOtp(String email, String purpose) {
        // Sinh ngẫu nhiên mã số 6 chữ số (100000 - 999999)
        String otp = String.format("%06d", RANDOM.nextInt(900000) + 100000);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Tạo và lưu entity OtpToken
        OtpToken otpToken = new OtpToken(email, otp, purpose, expiryTime);
        otpTokenRepository.save(otpToken);

        // Gửi qua MailService (vừa in Console vừa gửi Mail nếu cấu hình)
        mailService.sendOtpEmail(email, otp, purpose);

        return otp;
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        if (email == null || otp == null) {
            return false;
        }

        Optional<OtpToken> optionalToken = otpTokenRepository.findTopByEmailAndOtpAndIsUsedFalseOrderByExpiryTimeDesc(
                email.trim(),
                otp.trim()
        );

        if (optionalToken.isEmpty()) {
            return false;
        }

        OtpToken token = optionalToken.get();
        if (token.isExpired()) {
            return false;
        }

        // Đánh dấu đã sử dụng để tránh dùng lại (replay attack)
        token.setIsUsed(true);
        otpTokenRepository.save(token);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOtpValid(String email, String otp) {
        if (email == null || otp == null) {
            return false;
        }

        return otpTokenRepository.findTopByEmailAndOtpAndIsUsedFalseOrderByExpiryTimeDesc(
                email.trim(),
                otp.trim()
        ).filter(token -> !token.isExpired()).isPresent();
    }
}
