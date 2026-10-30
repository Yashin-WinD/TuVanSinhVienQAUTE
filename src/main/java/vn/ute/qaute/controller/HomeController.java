package vn.ute.qaute.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.service.DepartmentService;
import vn.ute.qaute.service.FaqService;
import vn.ute.qaute.service.UserService;

@Controller
public class HomeController {

    private final FaqService faqService;
    private final DepartmentService departmentService;
    private final UserService userService;

    @Autowired
    public HomeController(FaqService faqService,
                          DepartmentService departmentService,
                          UserService userService) {
        this.faqService = faqService;
        this.departmentService = departmentService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("topFaqs", faqService.getAllPublishedFaqs());
        return "public/index";
    }

    @GetMapping("/faq")
    public String faqPage(@RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "fieldId", required = false) Long fieldId,
                          Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("fields", departmentService.getAllFields());

        if (fieldId != null) {
            model.addAttribute("faqs", faqService.getFaqsByField(fieldId));
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("faqs", faqService.searchFaqs(keyword));
            model.addAttribute("searchKeyword", keyword);
        } else {
            model.addAttribute("faqs", faqService.getAllPublishedFaqs());
        }

        return "public/faq";
    }

    @PostMapping("/faq/{id}/view")
    @ResponseBody
    public ResponseEntity<Void> incrementFaqView(@PathVariable("id") Long id) {
        faqService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }
}
