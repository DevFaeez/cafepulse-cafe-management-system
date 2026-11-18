package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.ProductCategory;

import java.util.List;

public interface ProductCategoryService {

    // select all product category
    public List<ProductCategory> findAllProductCategory() throws Exception;

    // create new product catgegory
    public ProductCategory createNewProductCategory(String productCategoryName) throws Exception;

    // update the product category
    public ProductCategory updateProductCategory(Long productCategoryId, ProductCategory category) throws Exception;

    // delete
    public Long deleteProductCategory(Long productCategoryId) throws Exception;

    // find by id
    public ProductCategory findProductCategoryById(Long id) throws Exception;
}
