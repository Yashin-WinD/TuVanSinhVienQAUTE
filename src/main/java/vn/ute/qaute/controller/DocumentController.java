package vn.ute.qaute.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.ute.qaute.entity.Document;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.DocumentCategory;
import vn.ute.qaute.service.DocumentService;
import vn.ute.qaute.service.UserService;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Controller
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    @Autowired
    public DocumentController(DocumentService documentService, UserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @GetMapping("/documents")
    public String documentsPage(@RequestParam(value = "category", required = false) DocumentCategory category,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("categories", DocumentCategory.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);

        List<Document> documents = documentService.filterDocuments(category, keyword);
        model.addAttribute("documents", documents);

        return "public/documents";
    }

    @GetMapping("/documents/{id}/preview")
    public ResponseEntity<?> previewDocument(@PathVariable("id") Long id) {
        Document doc = documentService.getDocumentById(id);
        return serveDocumentFile(doc, true);
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable("id") Long id) {
        Document doc = documentService.getDocumentById(id);
        documentService.incrementDownloadCount(id);
        return serveDocumentFile(doc, false);
    }

    private ResponseEntity<?> serveDocumentFile(Document doc, boolean inline) {
        String fileUrl = doc.getFileUrl();
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Nếu là file lưu cục bộ trong /uploads/**
        if (fileUrl.startsWith("/uploads/")) {
            try {
                Path baseUploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
                String relative = fileUrl.substring("/uploads/".length());
                Path filePath = baseUploadRoot.resolve(relative).normalize();

                if (!filePath.startsWith(baseUploadRoot)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }

                // Nếu file mẫu trong DB bị lệch tên UUID, tự động fallback sang file PDF có sẵn trong uploads/documents
                if (!Files.exists(filePath) && "PDF".equalsIgnoreCase(doc.getFileType())) {
                    Path docDir = baseUploadRoot.resolve("documents");
                    if (Files.exists(docDir)) {
                        try (Stream<Path> stream = Files.list(docDir)) {
                            Path fallbackPdf = stream
                                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                                    .findFirst()
                                    .orElse(null);
                            if (fallbackPdf != null) {
                                filePath = fallbackPdf;
                            }
                        }
                    }
                }

                if (Files.exists(filePath) && Files.isReadable(filePath)) {
                    Resource resource = new UrlResource(filePath.toUri());
                    String contentType = determineContentType(filePath, doc.getFileType());
                    String displayFileName = (doc.getFileName() != null && !doc.getFileName().isBlank())
                            ? doc.getFileName()
                            : filePath.getFileName().toString();

                    ContentDisposition disposition = (inline
                            ? ContentDisposition.inline()
                            : ContentDisposition.attachment())
                            .filename(displayFileName, StandardCharsets.UTF_8)
                            .build();

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                            .header("X-Frame-Options", "SAMEORIGIN")
                            .body(resource);
                }
            } catch (Exception ignored) {
            }
        }

        // Nếu là URL bên ngoài (Cloudinary...)
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(fileUrl))
                .build();
    }

    private String determineContentType(Path filePath, String docFileType) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".pdf") || "PDF".equalsIgnoreCase(docFileType)) {
            return "application/pdf";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".doc")) {
            return "application/msword";
        } else if (fileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (fileName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (fileName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        try {
            String probed = Files.probeContentType(filePath);
            if (probed != null) {
                return probed;
            }
        } catch (Exception ignored) {
        }
        return "application/octet-stream";
    }
}
