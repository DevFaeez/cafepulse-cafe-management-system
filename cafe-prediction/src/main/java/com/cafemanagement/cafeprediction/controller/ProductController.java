package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.exception.AppException;
import com.cafemanagement.cafeprediction.exception.ProductCategoryNotFoundException;
import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.ProductCategory;
import com.cafemanagement.cafeprediction.repository.ProductCategoryRepository;
import com.cafemanagement.cafeprediction.request.CreateProductRequest;
import com.cafemanagement.cafeprediction.service.AuthService;
import com.cafemanagement.cafeprediction.service.ProductCategoryService;
import com.cafemanagement.cafeprediction.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProductCategoryService productCategoryService;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("")
    public ResponseEntity<List<Product>> findAllProduct() throws Exception {
        List<Product> productList = productService.findAllProduct();
        return new ResponseEntity<>(productList, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    @GetMapping("/available")
    public ResponseEntity<List<Product>> findAllProductAvailable(Authentication authentication) throws Exception {
        List<Product> productList = productService.findAllProductAvailable();
        return new ResponseEntity<>(productList, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/search")
    public ResponseEntity<?> findProductname(@RequestParam String productName) throws Exception {
        List<Product> productList = productService.findproductByName(productName);

        if (productList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No products found with name: " + productName);
        }

        return new ResponseEntity<List<Product>>(productList, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('MANAGER')")
    @PostMapping("/add")
    public ResponseEntity<Product> createNewProduct(@Valid @RequestBody CreateProductRequest req) throws Exception {
        ProductCategory productCategory = productCategoryService.findProductCategoryById(req.getProductCategory());

        Product createdNewProduct = productService.createNewProduct(req, productCategory);
        return new ResponseEntity<>(createdNewProduct, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('MANAGER')")
    @PutMapping("/{id}/update")
    public ResponseEntity<Product> updateProductDetails(@Valid @RequestBody CreateProductRequest request, @PathVariable Long id) throws Exception {
        Product savedProduct = productService.updateProductDetails(request, id);
        return new ResponseEntity<>(savedProduct, HttpStatus.OK);
    }


    @PreAuthorize("hasAnyRole('MANAGER')")
    @PutMapping("/{id}/updatestatus")
    public ResponseEntity<Product> updateProductStatus(@PathVariable Long id) throws Exception {
        Product savedProduct = productService.updateProductAvailability(id);
        return new ResponseEntity<>(savedProduct, HttpStatus.OK);
    }



    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}/sale-status")
    public Product setSaleStatus(@PathVariable Long id, @Valid @RequestBody SaleStatus request) {
        return productService.setProductAvailability(id, request.status());
    }

    public record SaleStatus(@jakarta.validation.constraints.NotNull com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS status) {}
}
