package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.ProductVariantCategory;
import jakarta.validation.Valid;
import com.cafemanagement.cafeprediction.request.ManageProductVariantCategoryRequest;
import com.cafemanagement.cafeprediction.service.ProductVaraintCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class ProductVariantCategoryController {

    @Autowired
    private ProductVaraintCategoryService productVaraintCategoryService;

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/productvariant/newcategory")
    public ResponseEntity<ProductVariantCategory> createNewProductvariantCategory(@Valid @RequestBody ManageProductVariantCategoryRequest req) throws Exception {
        ProductVariantCategory newProductCategroy = productVaraintCategoryService.createNewProductCategroy(req);
        return new ResponseEntity<>(newProductCategroy, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/productvariant/{id}/updatecategory")
    public ResponseEntity<ProductVariantCategory> updateProductvariantCategory(@Valid @RequestBody ManageProductVariantCategoryRequest req, @PathVariable Long id) throws Exception {
        ProductVariantCategory newProductCategroy = productVaraintCategoryService.editNewProductCategroy(req, id);
        return new ResponseEntity<>(newProductCategroy, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/productvariant")
    public ResponseEntity<List<ProductVariantCategory>> getAllProductVaraintCategory() throws Exception {
        List<ProductVariantCategory> allProductVariant = productVaraintCategoryService.getAllProductVariant();
        return new ResponseEntity<>(allProductVariant, HttpStatus.OK);
    }

}
