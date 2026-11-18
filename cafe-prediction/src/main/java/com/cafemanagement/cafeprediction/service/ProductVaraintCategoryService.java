package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.ProductCategory;
import com.cafemanagement.cafeprediction.model.ProductVariant;
import com.cafemanagement.cafeprediction.model.ProductVariantCategory;
import com.cafemanagement.cafeprediction.request.ManageProductVariantCategoryRequest;

import java.util.List;

public interface ProductVaraintCategoryService {

    // create new product variant category
    public ProductVariantCategory createNewProductCategroy(ManageProductVariantCategoryRequest req) throws Exception;

    // update new product category details
    public ProductVariantCategory editNewProductCategroy(ManageProductVariantCategoryRequest req, Long id) throws Exception;

    // select all variant
    public List<ProductVariantCategory> getAllProductVariant() throws Exception;

    // find by id
    public ProductVariantCategory findProductvariantCategoryById(Long id) throws Exception;
}
