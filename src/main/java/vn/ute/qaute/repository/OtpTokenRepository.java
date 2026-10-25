package vn.ute.qaute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.entity.OtpToken;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByEmailOrderByExpiryTimeDesc(String email);

    Optional<OtpToken> findTopByEmailAndOtpAndIsUsedFalseOrderByExpiryTimeDesc(String email, String otp);

    @Transactional
    void deleteByEmail(String email);
}
