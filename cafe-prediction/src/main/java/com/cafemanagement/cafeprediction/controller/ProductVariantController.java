package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.ProductVariant;
import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import com.cafemanagement.cafeprediction.service.ProductService;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;
import com.cafemanagement.cafeprediction.request.CreateProductVariantRequest;
import com.cafemanagement.cafeprediction.service.ProductVariantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class ProductVariantController {

    @Autowired
    private ProductVariantService productVariantService;

    @Autowired
    private ProductService productService;

    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    @GetMapping("/productvariant/{id}")
    public ResponseEntity<List<ProductVariant>> getAllProductVariant(@PathVariable Long id, Authentication auth) throws Exception {
        checkAvailability(productService.findProductById(id).getStatus(), auth);

        List<ProductVariant> productVariants = productVariantService.fetchAllProductVariantById(id);
        return new ResponseEntity<>(productVariants, HttpStatus.OK);

    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/productvariant/create")
    public ResponseEntity<ProductVariant> createNewProductVariant(@Valid @RequestBody CreateProductVariantRequest req) throws Exception {
        ProductVariant productVariant = productVariantService.createNewProductVariant(req);
        return new ResponseEntity<>(productVariant, HttpStatus.CREATED);

    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    @GetMapping("productvariant/get/{id}")
    public ResponseEntity<ProductVariant> getProductvariant(@PathVariable Long id, Authentication auth) throws Exception {
        ProductVariant variant = productVariantService.findProductVariantById(id);
        checkAvailability(variant.getProduct().getStatus(), auth);
        return new ResponseEntity<>(variant, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/productvariant/{id}")
    public ProductVariant update(@PathVariable Long id, @Valid @RequestBody CreateProductVariantRequest req) throws Exception {
        return productVariantService.updateProductVariant(req, id);
    }

    private void checkAvailability(PRODUCTS_STATUS status, Authentication auth) {
        if (status != PRODUCTS_STATUS.PRODUCT_AVAILABLE
                && auth.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_CUSTOMER"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is unavailable");
        }
    }
}
