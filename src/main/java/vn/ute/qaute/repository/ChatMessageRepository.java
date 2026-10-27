package vn.ute.qaute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ute.qaute.entity.ChatMessage;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConsultationIdOrderBySentAtAsc(Long consultationId);
    long countByConsultationIdAndSenderIdNotAndIsReadFalse(Long consultationId, Long currentUserId);
}
