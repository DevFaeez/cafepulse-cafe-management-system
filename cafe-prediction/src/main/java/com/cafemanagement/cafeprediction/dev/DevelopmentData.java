package com.cafemanagement.cafeprediction.dev;

import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.ProductCategory;
import com.cafemanagement.cafeprediction.model.enumoption.SALES_UNIT;
import com.cafemanagement.cafeprediction.model.ProductVariant;
import com.cafemanagement.cafeprediction.model.ProductVariantCategory;
import com.cafemanagement.cafeprediction.model.Staff;
import com.cafemanagement.cafeprediction.model.enumoption.STAFF_ROLES;
import com.cafemanagement.cafeprediction.repository.AuthRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("dev-seed")
class DevelopmentData implements ApplicationRunner {
    private static final String DRINKS = "Sample Drinks";
    private static final String SIZE = "Sample Size";
    private static final String LARGE = "Large";

    private final EntityManager entities;
    private final AuthRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    DevelopmentData(EntityManager entities, AuthRepository accounts, PasswordEncoder passwordEncoder,
                    @Value("${CAFE_DEV_SEED_PASSWORD}") String password) {
        this.entities = entities;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password.length() < 12) {
            throw new IllegalArgumentException("CAFE_DEV_SEED_PASSWORD must be at least 12 characters");
        }
        ProductCategory drinks = entities.createQuery(
                        "select c from ProductCategory c where c.name in :names order by c.id", ProductCategory.class)
                .setParameter("names", List.of(DRINKS, "Minuman Ujian")).setMaxResults(1).getResultList().stream().findFirst()
                .orElseGet(() -> {
                    ProductCategory category = new ProductCategory();
                    category.setName(DRINKS);
                    entities.persist(category);
                    return category;
                });
        Product latte = product(drinks, "Latte", "Espresso with milk.", "10.00");
        Product americano = product(drinks, "Americano", "Espresso with water.", "8.00");
        if (latte.getSalesUnit() == null) latte.setSalesUnit(SALES_UNIT.CUP);
        if (americano.getSalesUnit() == null) americano.setSalesUnit(SALES_UNIT.CUP);
        if (latte.getProductCategory().getId().equals(drinks.getId())) addLargeSize(latte);
        account("manager", "Demo Manager", "Pengurus Ujian", STAFF_ROLES.ROLE_MANAGER);
        account("staff", "Demo Staff", "Staf Ujian", STAFF_ROLES.ROLE_STAFF);
        account("kiosk", "Demo Kiosk", "Kiosk Ujian", STAFF_ROLES.ROLE_CUSTOMER);
    }

    private Product product(ProductCategory category, String name, String description, String price) {
        return entities.createQuery(
                        "select p from Product p where p.name = :name order by p.id", Product.class)
                .setParameter("name", name)
                .setMaxResults(1).getResultList().stream().findFirst()
                .orElseGet(() -> {
                    Product product = new Product();
                    product.setProductCategory(category);
                    product.setName(name);
                    product.setDescription(description);
                    product.setPrice(new BigDecimal(price));
                    product.setProductImage("/vite.svg");
                    entities.persist(product);
                    return product;
                });
    }

    private void addLargeSize(Product latte) {
        ProductVariantCategory size = entities.createQuery(
                        "select c from ProductVariantCategory c where c.name in :names order by c.id",
                        ProductVariantCategory.class)
                .setParameter("names", List.of(SIZE, "Saiz Ujian")).setMaxResults(1).getResultList().stream().findFirst()
                .orElseGet(() -> {
                    ProductVariantCategory category = new ProductVariantCategory();
                    category.setName(SIZE);
                    entities.persist(category);
                    return category;
                });
        boolean exists = !entities.createQuery("""
                        select v.id from ProductVariant v
                        where v.product = :product and v.category = :category and v.value in :values
                        """, Long.class)
                .setParameter("product", latte).setParameter("category", size).setParameter("values", List.of(LARGE, "Besar"))
                .setMaxResults(1).getResultList().isEmpty();
        if (!exists) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(latte);
            variant.setCategory(size);
            variant.setValue(LARGE);
            variant.setExtraPrice(new BigDecimal("2.00"));
            entities.persist(variant);
        }
    }

    private void account(String username, String fullName, String previousName, STAFF_ROLES role) {
        String email = username + "@cafepulse.example";
        var existing = accounts.findByEmail(email);
        if (existing.isPresent()) {
            Staff account = existing.get();
            if ((username + "-dev").equals(account.getUsername()) && previousName.equals(account.getFullname())) {
                account.setFullname(fullName);
            }
        } else {
            Staff account = new Staff();
            account.setUsername(username + "-dev");
            account.setFullname(fullName);
            account.setEmail(email);
            account.setRole(role);
            account.setPassword(passwordEncoder.encode(password));
            accounts.save(account);
        }
    }
}
