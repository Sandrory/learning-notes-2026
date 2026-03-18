package com.example.demo.controller;

import com.example.demo.domain.entity.Product;
import com.example.demo.domain.enums.ProductStatus;
import com.example.demo.dto.ProductResponse;
import com.example.demo.service.ProductService;
import com.example.demo.service.strategy.ProductConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * 商品管理控制器
 * 提供分页查询接口
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 分页查询商品
     * 所有认证用户都可以查询商品
     *
     * @param page 页码
     * @param size 每页大小
     * @param strategy 查询策略
     * @param params 查询参数
     * @return 分页商品数据
     */
    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> findProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam String strategy,
            @RequestParam(required = false) String params) {

        // 创建分页参数
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // 准备查询参数
        Object[] queryParams = parseQueryParams(strategy, params);

        // 执行查询
        Page<Product> productPage = productService.findProducts(pageable, strategy, queryParams);

        // 转换为DTO响应
        Page<ProductResponse> responsePage = convertToProductResponsePage(productPage, pageable);

        return ResponseEntity.ok(responsePage);
    }

    /**
     * 根据ID查询商品
     */
    @GetMapping("/id")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> findProductById(@RequestParam Long id) {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Product> productPage = productService.findProducts(
            pageable,
            ProductConstants.PRODUCT_QUERY_BY_ID,
            id
        );

        if (productPage.hasContent()) {
            Product product = productPage.getContent().get(0);
            return ResponseEntity.ok(convertToProductResponse(product));
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * 根据状态查询商品
     */
    @GetMapping("/by-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> findProductsByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.findProducts(
            pageable,
            ProductConstants.PRODUCT_QUERY_BY_STATUS,
            ProductStatus.fromCode(status)
        );

        Page<ProductResponse> responsePage = convertToProductResponsePage(productPage, pageable);
        return ResponseEntity.ok(responsePage);
    }

    /**
     * 查询可销售的商品（上架且有库存）
     */
    @GetMapping("/active-for-sale")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> findActiveForSale(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.findProducts(
            pageable,
            ProductConstants.PRODUCT_QUERY_ACTIVE_FOR_SALE
        );

        Page<ProductResponse> responsePage = convertToProductResponsePage(productPage, pageable);
        return ResponseEntity.ok(responsePage);
    }

    /**
     * 查询库存预警商品（库存低于阈值）
     */
    @GetMapping("/need-reorder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> findProductsNeedReorder(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.findProducts(
            pageable,
            ProductConstants.PRODUCT_QUERY_NEED_REORDER,
            threshold
        );

        Page<ProductResponse> responsePage = convertToProductResponsePage(productPage, pageable);
        return ResponseEntity.ok(responsePage);
    }

    /**
     * 解析查询参数
     */
    private Object[] parseQueryParams(String strategy, String params) {
        if (params == null || params.isEmpty()) {
            return new Object[]{};
        }

        try {
            switch (strategy) {
                case ProductConstants.PRODUCT_QUERY_BY_ID:
                    return new Object[]{Long.valueOf(params)};
                case ProductConstants.PRODUCT_QUERY_BY_NAME:
                case ProductConstants.PRODUCT_QUERY_BY_NAME_LIKE:
                    return new Object[]{params};
                case ProductConstants.PRODUCT_QUERY_BY_STATUS:
                    return new Object[]{ProductStatus.fromCode(params)};
                case ProductConstants.PRODUCT_QUERY_BY_PRICE_RANGE:
                    // 格式: min-max 或 min,max
                    String[] priceRange = params.split("[-,]");
                    if (priceRange.length == 2) {
                        return new Object[]{
                            new BigDecimal(priceRange[0]),
                            new BigDecimal(priceRange[1])
                        };
                    }
                    break;
                case ProductConstants.PRODUCT_QUERY_BY_STOCK:
                    return new Object[]{Integer.valueOf(params)};
                case ProductConstants.PRODUCT_QUERY_NEED_REORDER:
                    return new Object[]{Integer.valueOf(params)};
                default:
                    return new Object[]{};
            }
        } catch (Exception e) {
            // 参数格式错误，返回空参数
            return new Object[]{};
        }

        return new Object[]{};
    }

    /**
     * 转换Product到ProductResponse
     */
    private ProductResponse convertToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }

    /**
     * 转换Page<Product>到Page<ProductResponse>
     */
    private Page<ProductResponse> convertToProductResponsePage(Page<Product> productPage, Pageable pageable) {
        return new PageImpl<ProductResponse>(
                productPage.getContent().stream()
                        .map(this::convertToProductResponse)
                        .collect(Collectors.toList()),
                pageable,
                productPage.getTotalElements()
        );
    }
}
