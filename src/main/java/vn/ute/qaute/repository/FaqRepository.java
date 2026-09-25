package vn.ute.qaute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.ute.qaute.entity.Faq;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByIsPublishedTrueOrderByViewCountDesc();
    List<Faq> findByFieldIdAndIsPublishedTrue(Long fieldId);

    @Query("SELECT f FROM Faq f WHERE f.isPublished = true AND " +
           "(LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.answer) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Faq> searchByKeyword(@Param("keyword") String keyword);
}
