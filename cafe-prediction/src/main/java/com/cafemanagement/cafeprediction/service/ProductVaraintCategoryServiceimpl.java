package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.exception.ProductVariantCategoryNotFoundException;
import com.cafemanagement.cafeprediction.model.ProductVariantCategory;
import com.cafemanagement.cafeprediction.repository.ProductVaraintCategoryRepository;
import com.cafemanagement.cafeprediction.repository.ProductVariantRepository;
import com.cafemanagement.cafeprediction.request.ManageProductVariantCategoryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductVaraintCategoryServiceimpl implements ProductVaraintCategoryService {

    @Autowired
    private ProductVaraintCategoryRepository productVaraintCategoryRepository;

    @Override
    public ProductVariantCategory createNewProductCategroy(ManageProductVariantCategoryRequest req) throws Exception {
        ProductVariantCategory productVariantCategory = new ProductVariantCategory();
        productVariantCategory.setName(req.getName());

        return productVaraintCategoryRepository.save(productVariantCategory);

    }

    @Override
    public ProductVariantCategory editNewProductCategroy(ManageProductVariantCategoryRequest req, Long id) throws Exception {
        ProductVariantCategory productvariantCategoryById = findProductvariantCategoryById(id);
        productvariantCategoryById.setName(req.getName());


        return productVaraintCategoryRepository.save(productvariantCategoryById);
    }

    @Override
    public List<ProductVariantCategory> getAllProductVariant() throws Exception {
        List<ProductVariantCategory> variantCategoryList = productVaraintCategoryRepository.findAll();
        if (variantCategoryList.isEmpty()) {
            throw new Exception("avaialble product variant is null");
        }
        return variantCategoryList;
    }

    @Override
    public ProductVariantCategory findProductvariantCategoryById(Long id) throws Exception {
        Optional<ProductVariantCategory> optionalProductVariantCategory = productVaraintCategoryRepository.findById(id);
        if (optionalProductVariantCategory.isEmpty()) {
            throw new ProductVariantCategoryNotFoundException("product variant category not found");
        }
        return optionalProductVariantCategory.get();
    }

}
