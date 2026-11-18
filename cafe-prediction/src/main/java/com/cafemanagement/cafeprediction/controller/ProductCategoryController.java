package com.cafemanagement.cafeprediction.controller;


import com.cafemanagement.cafeprediction.model.ProductCategory;
import com.cafemanagement.cafeprediction.response.MessageResponse;
import com.cafemanagement.cafeprediction.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class ProductCategoryController {

    @Autowired
    private ProductCategoryService productCategoryService;

    // find all product category
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @GetMapping("/productcategory")
    public ResponseEntity<?> findAllProductCategory() throws Exception {
        List<ProductCategory> allProductCategory = productCategoryService.findAllProductCategory();

        return new ResponseEntity<List<ProductCategory>>(allProductCategory, HttpStatus.OK);
    }

    // create new product category endpoint
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PostMapping("/productcategory/new")
    public ResponseEntity<ProductCategory> createNewProductCategory(@RequestBody Map<String, String> request) throws Exception {
        String productCategoryName = title(request.get("productCategoryName"));
        ProductCategory newProductCategory = productCategoryService.createNewProductCategory(productCategoryName);
        return new ResponseEntity<>(newProductCategory, HttpStatus.CREATED);
    }


    // update product category endpoint
    @PreAuthorize(("hasAnyRole('MANAGER')"))
    @PutMapping("/productcategory/{id}/update")
    public ResponseEntity<ProductCategory> updateProductCategory(@PathVariable Long id, @RequestBody ProductCategory category) throws Exception {
        category.setName(title(category.getName()));
        ProductCategory updateProductCategory = productCategoryService.updateProductCategory(id, category);
        return new ResponseEntity<>(updateProductCategory, HttpStatus.OK);
    }

    private static String title(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a category name of up to 100 characters.");
        }
        return value.strip();
    }

    // delete product category endpoint
    @PreAuthorize(("hasAnyRole('MANAGER')"))
    @DeleteMapping("/productcategory/{id}/delete")
    public ResponseEntity<MessageResponse> deleteProductCategory(@PathVariable Long id) throws Exception {
        productCategoryService.deleteProductCategory(id);

        MessageResponse messageResponse = MessageResponse.builder()
                .status("delete_product_category_success")
                .message("Product Category Successfull Delete")
                .build();

        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }

}
