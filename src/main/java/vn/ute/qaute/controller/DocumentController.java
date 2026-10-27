package vn.ute.qaute.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;

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

    @GetMapping("/documents/{id}/download")
    public String downloadDocument(@PathVariable("id") Long id) {
        Document doc = documentService.getDocumentById(id);
        documentService.incrementDownloadCount(id);
        return "redirect:" + doc.getFileUrl();
    }
}
