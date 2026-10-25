package vn.ute.qaute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.ute.qaute.entity.Document;
import vn.ute.qaute.entity.enums.DocumentCategory;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByCreatedAtDesc();

    List<Document> findByCategoryOrderByCreatedAtDesc(DocumentCategory category);

    @Query("SELECT d FROM Document d WHERE " +
           "(:category IS NULL OR d.category = :category) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY d.createdAt DESC")
    List<Document> filterDocuments(@Param("category") DocumentCategory category, @Param("keyword") String keyword);
}
