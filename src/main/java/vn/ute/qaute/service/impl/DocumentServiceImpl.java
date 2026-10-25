package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.ute.qaute.dto.response.CloudinaryResponseDto;
import vn.ute.qaute.entity.Document;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.DocumentCategory;
import vn.ute.qaute.exception.BadRequestException;
import vn.ute.qaute.exception.ResourceNotFoundException;
import vn.ute.qaute.repository.DocumentRepository;
import vn.ute.qaute.service.CloudinaryService;
import vn.ute.qaute.service.DocumentService;

import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final CloudinaryService cloudinaryService;

    @Autowired
    public DocumentServiceImpl(DocumentRepository documentRepository,
                               CloudinaryService cloudinaryService) {
        this.documentRepository = documentRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    @Transactional
    public Document uploadDocument(String title, String description, DocumentCategory category, 
                                  MultipartFile file, User uploader) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn tệp tin văn bản hoặc biểu mẫu cần tải lên!");
        }

        String originalFilename = file.getOriginalFilename();
        String fileType = "UNKNOWN";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toUpperCase();
        }

        CloudinaryResponseDto uploadRes = cloudinaryService.uploadImage(file, "documents");
        if (uploadRes == null || uploadRes.getUrl() == null) {
            throw new BadRequestException("Không thể lưu trữ tệp tin. Vui lòng thử lại!");
        }

        Document doc = new Document(
                title,
                description,
                uploadRes.getUrl(),
                originalFilename,
                fileType,
                file.getSize(),
                category,
                uploader
        );

        return documentRepository.save(doc);
    }

    @Override
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<Document> filterDocuments(DocumentCategory category, String keyword) {
        return documentRepository.filterDocuments(category, keyword);
    }

    @Override
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu văn bản với ID: " + id));
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementDownloadCount(Long id) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setDownloadCount(doc.getDownloadCount() + 1);
            documentRepository.save(doc);
        });
    }
}
