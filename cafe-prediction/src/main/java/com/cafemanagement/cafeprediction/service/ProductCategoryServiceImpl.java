package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.exception.AppException;
import com.cafemanagement.cafeprediction.exception.ProductCategoryNotFoundException;
import com.cafemanagement.cafeprediction.model.ProductCategory;
import com.cafemanagement.cafeprediction.repository.ProductCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Override
    public List<ProductCategory> findAllProductCategory() throws Exception {
        List<ProductCategory> productCategories = productCategoryRepository.findAll();
        if (productCategories.isEmpty()) {
            throw new Exception("avaialble product is null");
        }
        return productCategories;
    }

    @Override
    public ProductCategory createNewProductCategory(String productCategoryName) throws Exception {

        ProductCategory productCategory = new ProductCategory();
        productCategory.setName(productCategoryName);

        ProductCategory savedProductCategory = productCategoryRepository.save(productCategory);

        return savedProductCategory;
    }

    @Override
    public ProductCategory updateProductCategory(Long productCategoryId, ProductCategory category) throws Exception {
        ProductCategory productCategory = findProductCategoryById(productCategoryId);
        productCategory.setName(category.getName());
        return productCategoryRepository.save(productCategory);
    }

    @Override
    public Long deleteProductCategory(Long productCategoryId) throws Exception {
        ProductCategory productCategory = findProductCategoryById(productCategoryId);
        productCategoryRepository.delete(productCategory);

        return productCategory.getId();
    }

    @Override
    public ProductCategory findProductCategoryById(Long id) throws Exception {
        Optional<ProductCategory> optionalProductCategory = productCategoryRepository.findById(id);
        if (optionalProductCategory.isEmpty()) {
            throw new ProductCategoryNotFoundException("Product Category did not exist...");
        }
        return optionalProductCategory.get();
    }


}
