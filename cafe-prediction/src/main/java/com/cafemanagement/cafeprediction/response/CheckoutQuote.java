package com.cafemanagement.cafeprediction.response;

import java.math.BigDecimal;

public record CheckoutQuote(BigDecimal subtotal, BigDecimal promoDiscount, BigDecimal pointsDiscount, BigDecimal totalAmount,
                            int availablePoints, int pointsRedeemed, int pointsEarned, String promoCode) {}
