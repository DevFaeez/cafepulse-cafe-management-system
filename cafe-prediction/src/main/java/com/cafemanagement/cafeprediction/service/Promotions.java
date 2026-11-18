package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.model.Promo;
import com.cafemanagement.cafeprediction.repository.ProductRepository;
import com.cafemanagement.cafeprediction.repository.PromoRepository;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class Promotions {
    public enum Type { PERCENT, FIXED, BOGO }
    private final PromoRepository promos;
    private final ProductRepository products;
    private final ZoneId zone;

    public Promotions(PromoRepository promos, ProductRepository products, @Value("${cafe.business-zone}") String zone) {
        this.promos = promos;
        this.products = products;
        this.zone = ZoneId.of(zone);
    }

    public List<Promo> all() {
        return promos.findAllByOrderByIdDesc();
    }

    public List<Offer> active() {
        LocalDateTime now = LocalDateTime.now(zone);
        return promos.findByCodeIsNotNullAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanOrderByCode(now, now)
                .stream().map(promo -> new Offer(promo.getCode(), promo.getName(),
                        promo.getEligibleProduct() == null ? null : promo.getEligibleProduct().getId())).toList();
    }

    @Transactional
    public Promo save(Long id, Input input) {
        if (!input.endDate().isAfter(input.startDate())) {
            throw badRequest("The end time must be after the start time.");
        }
        if ((input.type() == Type.BOGO && input.productId() == null)
                || (input.type() != Type.BOGO && (input.value() == null || input.value().signum() <= 0))
                || (input.type() == Type.PERCENT && input.value().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw badRequest("Check the discount amount and eligible products.");
        }
        Promo promo = id == null ? new Promo() : promos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found."));
        promo.setCode(code(input.code()));
        promo.setName(input.name().strip());
        promo.setDiscountType(input.type().name());
        promo.setDiscountValue(input.type() == Type.BOGO ? "0" : input.value().toPlainString());
        promo.setEligibleProduct(input.productId() == null ? null : products.findById(input.productId())
                .orElseThrow(() -> badRequest("The product for this promotion could not be found.")));
        promo.setStartDate(input.startDate());
        promo.setEndDate(input.endDate());
        promo.setIsActive(input.active());
        try {
            return promos.saveAndFlush(promo);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This promo code is already in use.");
        }
    }

    public BigDecimal discount(String code, List<OrderItem> items) {
        if (code.isEmpty()) return BigDecimal.ZERO;
        var promo = promos.findByCode(code).orElseThrow(() -> badRequest("Promo code not found."));
        LocalDateTime now = LocalDateTime.now(zone);
        if (!Boolean.TRUE.equals(promo.getIsActive())) throw badRequest("This promotion is inactive.");
        if (now.isBefore(promo.getStartDate())) throw badRequest("This promotion hasn't started yet.");
        if (!now.isBefore(promo.getEndDate())) throw badRequest("This promotion has ended.");
        var eligible = items.stream().filter(item -> promo.getEligibleProduct() == null
                || item.getProduct().getId().equals(promo.getEligibleProduct().getId())).toList();
        if (eligible.isEmpty()) throw badRequest("Your cart has no products eligible for this promotion.");
        if (Type.BOGO.name().equals(promo.getDiscountType())) {
            int cups = eligible.stream().mapToInt(OrderItem::getQuantity).sum();
            if (cups < 2) throw badRequest("Add two eligible items to use this promotion.");
            return eligible.get(0).getPrice().multiply(BigDecimal.valueOf(cups / 2));
        }
        BigDecimal subtotal = eligible.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal value = new BigDecimal(promo.getDiscountValue());
        if (Type.PERCENT.name().equals(promo.getDiscountType())) {
            return subtotal.multiply(value).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        }
        return subtotal.min(value);
    }

    public static String code(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record Input(@NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{2,24}") String code,
                        @NotBlank @Size(max = 100) String name, @NotNull Type type,
                        @Digits(integer = 8, fraction = 2) BigDecimal value, @Positive Long productId,
                        @NotNull LocalDateTime startDate, @NotNull LocalDateTime endDate, @NotNull Boolean active) {}
    public record Offer(String code, String name, Long productId) {}
}
