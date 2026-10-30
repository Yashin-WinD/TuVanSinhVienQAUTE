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

        try {
            Path baseUploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
            Path filePath = null;

            if (fileUrl.startsWith("/uploads/")) {
                String relative = fileUrl.substring("/uploads/".length());
                filePath = baseUploadRoot.resolve(relative).normalize();
            } else if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                // Nếu URL là link Cloudinary (https://res.cloudinary.com/.../qaute/documents/ten_file.pdf),
                // ưu tiên đọc từ bản lưu đồng bộ tại uploads/documents/ten_file.pdf để không bị lỗi 401 PDF của Cloudinary Free
                int lastSlash = fileUrl.lastIndexOf('/');
                if (lastSlash > 0 && lastSlash < fileUrl.length() - 1) {
                    String cldFileName = fileUrl.substring(lastSlash + 1).replaceAll("[^a-zA-Z0-9._-]", "");
                    if (!cldFileName.isEmpty()) {
                        Path candidate = baseUploadRoot.resolve("documents").resolve(cldFileName).normalize();
                        if (candidate.startsWith(baseUploadRoot) && Files.exists(candidate)) {
                            filePath = candidate;
                        }
                    }
                }
            }

            if (filePath != null && !filePath.startsWith(baseUploadRoot)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Nếu file mẫu trong DB chưa có trên đĩa, fallback sang file PDF có sẵn trong uploads/documents
            if ((filePath == null || !Files.exists(filePath)) && "PDF".equalsIgnoreCase(doc.getFileType())) {
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

            if (filePath != null && Files.exists(filePath) && Files.isReadable(filePath)) {
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

        // Nếu không có bản lưu cục bộ thì chuyển hướng sang URL gốc
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
