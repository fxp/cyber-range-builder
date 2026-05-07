package com.cyberrange.pointsmall.service;

import com.cyberrange.pointsmall.cache.CacheService;
import com.cyberrange.pointsmall.model.Product;
import com.cyberrange.pointsmall.repository.ProductRepository;
import com.cyberrange.pointsmall.search.ProductSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String PRODUCT_CACHE_KEY = "product:";
    private static final int PRODUCT_CACHE_TTL = 1800;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ProductSearchService searchService;

    public Page<Product> listProducts(Pageable pageable) {
        return productRepository.findByStatus(Product.ProductStatus.ON_SALE, pageable);
    }

    public Page<Product> listByCategory(Product.ProductCategory category, Pageable pageable) {
        return productRepository.findByStatusAndCategory(Product.ProductStatus.ON_SALE, category, pageable);
    }

    public Product getProduct(Long productId) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        return cacheService.get(cacheKey, Product.class)
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new RuntimeException("商品不存在"));
                    cacheService.set(cacheKey, product, PRODUCT_CACHE_TTL);
                    return product;
                });
    }

    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        String processedQuery = searchService.buildSearchQuery(keyword);
        log.debug("Processed search query: {} -> {}", keyword, processedQuery);
        return productRepository.searchProducts(processedQuery, pageable);
    }

    public List<Product> getHotProducts() {
        return productRepository.findTop10ByStatusOrderBySoldCountDesc(Product.ProductStatus.ON_SALE);
    }

    @Transactional
    public Product createProduct(Product product) {
        if (product.getTags() != null && !product.getTags().isEmpty()) {
            String keywords = searchService.extractKeywords(product.getDescription() + " " + product.getName(), 5);
            product.setTags(product.getTags() + "," + keywords);
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long productId, Product updates) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        if (updates.getName() != null) product.setName(updates.getName());
        if (updates.getDescription() != null) product.setDescription(updates.getDescription());
        if (updates.getPointsPrice() != null) product.setPointsPrice(updates.getPointsPrice());
        if (updates.getStock() != null) product.setStock(updates.getStock());
        if (updates.getStatus() != null) product.setStatus(updates.getStatus());

        Product saved = productRepository.save(product);
        cacheService.delete(PRODUCT_CACHE_KEY + productId);
        return saved;
    }

    public List<String> getSearchSuggestions(String keyword) {
        return searchService.suggestRelatedTerms(keyword);
    }
}
