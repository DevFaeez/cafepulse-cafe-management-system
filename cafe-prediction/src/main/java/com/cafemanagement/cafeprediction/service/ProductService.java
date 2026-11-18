package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.ProductCategory;
import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import com.cafemanagement.cafeprediction.request.CreateProductRequest;

import java.util.List;

public interface ProductService {

    // Select all product: for admin
    public List<Product> findAllProduct() throws Exception;

    // Select all proudct avaiable: for user display
    public List<Product> findAllProductAvailable() throws Exception;

    // search product by name
    public List<Product> findproductByName(String productName) throws Exception;

    // add new product
    public Product createNewProduct(CreateProductRequest request, ProductCategory productCategory) throws Exception;

    // update product details
    public Product updateProductDetails(CreateProductRequest request, Long id) throws Exception;

    // update product availability
    public Product updateProductAvailability(Long id) throws Exception;

    Product setProductAvailability(Long id, PRODUCTS_STATUS status);

    public Product findProductById(Long id);




}
