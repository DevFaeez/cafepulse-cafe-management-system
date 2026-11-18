package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.exception.ProductVariantNotFound;
import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.ProductVariant;
import com.cafemanagement.cafeprediction.model.ProductVariantCategory;
import com.cafemanagement.cafeprediction.repository.ProductVariantRepository;
import com.cafemanagement.cafeprediction.request.CreateProductVariantRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductVariantServiceImpl implements ProductVariantService {

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductVaraintCategoryService productVaraintCategoryService;

    @Autowired
    private ProductService productService;

    @Override
    public List<ProductVariant> fetchAllProductVariantById(Long id) throws Exception {
        return productVariantRepository.findByProductId(id);
    }


    @Transactional
    @Override
    public ProductVariant createNewProductVariant(CreateProductVariantRequest req) throws Exception {

        // find product
        Product product = productService.findProductById(req.getProduct());

        // find category
        ProductVariantCategory category = productVaraintCategoryService.findProductvariantCategoryById(req.getCategory());

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setCategory(category);
        productVariant.setValue(req.getValue());
        productVariant.setExtraPrice(req.getExtraPrice());

        ProductVariant variant = productVariantRepository.save(productVariant);
        return variant;
    }

    @Transactional
    @Override
    public ProductVariant updateProductVariant(CreateProductVariantRequest req, Long id) throws Exception {
        ProductVariant productVariant = findProductVariantById(id);

        // find product
        Product product = productService.findProductById(req.getProduct());

        // find category
        ProductVariantCategory category = productVaraintCategoryService.findProductvariantCategoryById(req.getCategory());

        productVariant.setProduct(product);
        productVariant.setCategory(category);
        productVariant.setValue(req.getValue());
        productVariant.setExtraPrice(req.getExtraPrice());

        ProductVariant savedProductVariant = productVariantRepository.save(productVariant);

        return savedProductVariant;
    }

    @Override
    public ProductVariant findProductVariantById(Long id) throws Exception {
        Optional<ProductVariant> productVariant = productVariantRepository.findById(id);
        if (productVariant.isEmpty()) {
            throw new ProductVariantNotFound("product variant not found...");
        }
        return productVariant.get();
    }

}
