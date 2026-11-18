package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.ProductVariant;
import com.cafemanagement.cafeprediction.request.CreateProductVariantRequest;

import java.util.List;

public interface ProductVariantService {

    // select variant
    public List<ProductVariant> fetchAllProductVariantById(Long id) throws Exception;

    // create new variant
    public ProductVariant createNewProductVariant(CreateProductVariantRequest req) throws Exception;

    // edit varaint
    public ProductVariant updateProductVariant(CreateProductVariantRequest request, Long id) throws Exception;

    // get product variant for single variant
    public ProductVariant findProductVariantById(Long id) throws Exception;

    // get all product variant from product id

}
