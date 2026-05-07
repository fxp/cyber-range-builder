package com.cyberrange.pointsmall.search;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    public List<String> tokenize(String text) {
        List<Term> terms = HanLP.segment(text);
        return terms.stream()
                .map(term -> term.word)
                .filter(word -> word.length() > 1)
                .distinct()
                .collect(Collectors.toList());
    }

    public String extractKeywords(String text, int topN) {
        List<String> keywords = HanLP.extractKeyword(text, topN);
        return String.join(",", keywords);
    }

    public String extractSummary(String text, int maxLength) {
        List<String> sentences = HanLP.extractSummary(text, 3);
        String summary = String.join("。", sentences);
        if (summary.length() > maxLength) {
            summary = summary.substring(0, maxLength) + "...";
        }
        return summary;
    }

    public String convertToPinyin(String text) {
        return HanLP.convertToPinyinString(text, "", false);
    }

    public boolean containsKeyword(String text, String keyword) {
        List<String> tokens = tokenize(text);
        List<String> keywordTokens = tokenize(keyword);
        return tokens.stream().anyMatch(keywordTokens::contains);
    }

    public double calculateSimilarity(String text1, String text2) {
        List<String> tokens1 = tokenize(text1);
        List<String> tokens2 = tokenize(text2);

        long commonCount = tokens1.stream().filter(tokens2::contains).count();
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0;

        return (double) commonCount / Math.max(tokens1.size(), tokens2.size());
    }

    public String buildSearchQuery(String userInput) {
        List<String> keywords = tokenize(userInput);
        return keywords.stream()
                .filter(kw -> kw.length() >= 2)
                .collect(Collectors.joining(" "));
    }

    public List<String> suggestRelatedTerms(String text) {
        return HanLP.extractKeyword(text, 10);
    }
}
