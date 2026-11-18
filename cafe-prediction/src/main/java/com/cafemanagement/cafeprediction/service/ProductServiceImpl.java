package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.exception.ProductNotFoundException;
import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.ProductCategory;
import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import com.cafemanagement.cafeprediction.repository.ProductRepository;
import com.cafemanagement.cafeprediction.repository.ProductCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cafemanagement.cafeprediction.request.CreateProductRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductCategoryRepository categories;

    // find all product
    @Override
    public List<Product> findAllProduct() throws Exception {
        List<Product> productList = productRepository.findAll();

        if (productList == null) {
            throw new Exception("product is null");
        }

        return productList;
    }

    // find only avaiable product
    @Override
    public List<Product> findAllProductAvailable() throws Exception {
        List<Product> productList = productRepository.findAllByStatus(PRODUCTS_STATUS.PRODUCT_AVAILABLE);

        if (productList == null) {
            throw new Exception("available product is null");
        }

        return productList;
    }

    // find the product by the name
    @Override
    public List<Product> findproductByName(String productName) throws Exception {
        List<Product> products = productRepository.findByNameContaining(productName);
        return products;
    }

//    @Transactional(rollbackFor = Exception.class)
    @Transactional
    @Override
    public Product createNewProduct(CreateProductRequest request, ProductCategory productCategory) throws Exception {

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setProductImage(request.getProductImage());
        product.setPrice(request.getPrice());
        product.setStatus(request.getStatus());
        product.setSalesUnit(request.getSalesUnit());
        product.setProductCategory(productCategory);

        Product savedProduct = productRepository.save(product);
        productCategory.getProduct().add(savedProduct);
        return savedProduct;

    }

    @Override
    @Transactional
    public Product updateProductDetails(CreateProductRequest request, Long productId) throws Exception {
        Product product = lockProduct(productId);
        product.setProductImage(request.getProductImage());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        if (request.getSalesUnit() != null) product.setSalesUnit(request.getSalesUnit());
        if (request.getProductCategory() != null) {
            product.setProductCategory(categories.findById(request.getProductCategory())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product category not found.")));
        }

        Product savedProduct = productRepository.save(product);
        return savedProduct;
    }

    @Override
    @Transactional
    public Product updateProductAvailability(Long id) throws Exception {
        Product product = lockProduct(id);

        switch (product.getStatus()) {
            case PRODUCT_AVAILABLE -> product.setStatus(PRODUCTS_STATUS.PRODUCT_OUT_OF_STOCK);
            case PRODUCT_OUT_OF_STOCK -> product.setStatus(PRODUCTS_STATUS.PRODUCT_AVAILABLE);
            default -> throw new IllegalStateException("Unknown product status: " + product.getStatus());
        }

        Product savedRepository = productRepository.save(product);
        return savedRepository;
    }

    @Override
    @Transactional
    public Product setProductAvailability(Long id, PRODUCTS_STATUS status) {
        Product product = lockProduct(id);
        product.setStatus(status);
        return product;
    }

    private Product lockProduct(Long id) {
        return productRepository.lockForManagement(id)
                .orElseThrow(() -> new ProductNotFoundException("Product ID " + id + " not found"));
    }

    @Override
    public Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product ID " + id + " not found"));
    }


}
