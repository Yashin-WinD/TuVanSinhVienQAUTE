package vn.ute.qaute.service;

import org.springframework.web.multipart.MultipartFile;
import vn.ute.qaute.entity.Document;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.DocumentCategory;

import java.util.List;

public interface DocumentService {
    Document uploadDocument(String title, String description, DocumentCategory category, MultipartFile file, User uploader);
    List<Document> getAllDocuments();
    List<Document> filterDocuments(DocumentCategory category, String keyword);
    Document getDocumentById(Long id);
    void deleteDocument(Long id);
    void incrementDownloadCount(Long id);
}
