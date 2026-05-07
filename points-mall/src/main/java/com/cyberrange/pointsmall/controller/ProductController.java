package com.cyberrange.pointsmall.controller;

import com.cyberrange.pointsmall.dto.ApiResponse;
import com.cyberrange.pointsmall.model.Product;
import com.cyberrange.pointsmall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ApiResponse<Page<Product>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("soldCount").descending());

        if (category != null) {
            Product.ProductCategory cat = Product.ProductCategory.valueOf(category.toUpperCase());
            return ApiResponse.success(productService.listByCategory(cat, pageRequest));
        }

        return ApiResponse.success(productService.listProducts(pageRequest));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getProduct(@PathVariable Long id) {
        return ApiResponse.success(productService.getProduct(id));
    }

    @GetMapping("/search")
    public ApiResponse<Page<Product>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return ApiResponse.success(productService.searchProducts(keyword, pageRequest));
    }

    @GetMapping("/hot")
    public ApiResponse<List<Product>> getHotProducts() {
        return ApiResponse.success(productService.getHotProducts());
    }

    @GetMapping("/search/suggest")
    public ApiResponse<List<String>> getSearchSuggestions(@RequestParam String keyword) {
        return ApiResponse.success(productService.getSearchSuggestions(keyword));
    }
}
