package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.entity.Faq;
import vn.ute.qaute.exception.ResourceNotFoundException;
import vn.ute.qaute.repository.FaqRepository;
import vn.ute.qaute.service.FaqService;

import java.util.List;

@Service
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;

    @Autowired
    public FaqServiceImpl(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Override
    public List<Faq> getAllPublishedFaqs() {
        return faqRepository.findByIsPublishedTrueOrderByViewCountDesc();
    }

    @Override
    public List<Faq> getAllFaqsForAdmin() {
        return faqRepository.findAll();
    }

    @Override
    public List<Faq> getFaqsByField(Long fieldId) {
        return faqRepository.findByFieldIdAndIsPublishedTrue(fieldId);
    }

    @Override
    public List<Faq> searchFaqs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPublishedFaqs();
        }
        return faqRepository.searchByKeyword(keyword.trim());
    }

    @Override
    public Faq getFaqById(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi FAQ với ID: " + id));
    }

    @Override
    @Transactional
    public Faq createFaq(Faq faq) {
        return faqRepository.save(faq);
    }

    @Override
    @Transactional
    public Faq updateFaq(Long id, Faq faqDetails) {
        Faq faq = getFaqById(id);
        faq.setQuestion(faqDetails.getQuestion());
        faq.setAnswer(faqDetails.getAnswer());
        if (faqDetails.getField() != null) {
            faq.setField(faqDetails.getField());
        }
        if (faqDetails.getIsPublished() != null) {
            faq.setIsPublished(faqDetails.getIsPublished());
        }
        return faqRepository.save(faq);
    }

    @Override
    @Transactional
    public void deleteFaq(Long id) {
        faqRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        faqRepository.findById(id).ifPresent(f -> {
            f.setViewCount(f.getViewCount() + 1);
            faqRepository.save(f);
        });
    }
}
