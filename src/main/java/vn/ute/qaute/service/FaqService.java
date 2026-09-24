package vn.ute.qaute.service;

import vn.ute.qaute.entity.Faq;

import java.util.List;

public interface FaqService {
    List<Faq> getAllPublishedFaqs();
    List<Faq> getAllFaqsForAdmin();
    List<Faq> getFaqsByField(Long fieldId);
    List<Faq> searchFaqs(String keyword);
    Faq getFaqById(Long id);
    Faq createFaq(Faq faq);
    Faq updateFaq(Long id, Faq faq);
    void deleteFaq(Long id);
    void incrementViewCount(Long id);
}
