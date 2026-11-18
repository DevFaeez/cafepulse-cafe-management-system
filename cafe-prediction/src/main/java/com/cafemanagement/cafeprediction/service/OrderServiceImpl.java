package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Customer;
import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.model.OrderItemVariant;
import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.ProductVariant;
import com.cafemanagement.cafeprediction.model.enumoption.ORDER_STATUS;
import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import com.cafemanagement.cafeprediction.repository.OrderRepository;
import com.cafemanagement.cafeprediction.repository.ProductRepository;
import com.cafemanagement.cafeprediction.repository.ProductVariantRepository;
import com.cafemanagement.cafeprediction.request.CreateCustomerRequest;
import com.cafemanagement.cafeprediction.response.CheckoutQuote;
import com.cafemanagement.cafeprediction.request.OrderItemRequest;
import com.cafemanagement.cafeprediction.request.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal MAX_TOTAL = new BigDecimal("99999999.99");
    private final CustomerService customers;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final DailyOrderSequenceService sequences;
    private final OrderRewards rewards;
    private final Promotions promotions;
    private final ZoneId businessZone;
    private final ObjectMapper json;

    public OrderServiceImpl(CustomerService customers, OrderRepository orders, ProductRepository products,
                            ProductVariantRepository variants, DailyOrderSequenceService sequences,
                            OrderRewards rewards, Promotions promotions, ObjectMapper json, @Value("${cafe.business-zone}") String businessZone) {
        this.customers = customers;
        this.orders = orders;
        this.products = products;
        this.variants = variants;
        this.sequences = sequences;
        this.rewards = rewards;
        this.promotions = promotions;
        this.json = json;
        this.businessZone = ZoneId.of(businessZone);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Orders createNewOrder(String kiosk, UUID requestId, OrderRequest request) throws Exception {
        String phone = CustomerServiceImpl.normalizePhone(request.getCustomerPhone());
        List<CheckoutLine> lines = request.getItem().stream().map(CheckoutLine::of).toList();
        String key = digest(kiosk + "\n" + requestId);
        String code = Promotions.code(request.getPromoCode());
        List<?> identity = code.isEmpty() && request.getPoints() == 0
                ? List.of(phone, lines) : List.of(phone, lines, code, request.getPoints());
        String fingerprint = digest(json.writeValueAsString(identity));
        sequences.lockCheckout();
        var previous = orders.findByCheckoutKey(key);
        if (previous.isPresent()) {
            Orders order = previous.get();
            if (!fingerprint.equals(order.getCheckoutFingerprint())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This submission ID has already been used for a different cart.");
            }
            return order;
        }

        Orders order = assemble(lines);
        CheckoutQuote quote = price(order, customers.findByNormalizedPhone(phone).orElse(null), code, request.getPoints());
        if (request.getExpectedTotal() != null && request.getExpectedTotal().compareTo(quote.totalAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The price changed. Review the updated order before submitting.");
        }

        CreateCustomerRequest contact = new CreateCustomerRequest();
        contact.setPhoneNumber(phone);
        Customer customer = customers.createNewCustomer(contact);
        Instant acceptedAt = Instant.now();
        LocalDate today = LocalDate.ofInstant(acceptedAt, businessZone);
        order.setCustomer(customer);
        order.setCheckoutKey(key);
        order.setCheckoutFingerprint(fingerprint);
        order.setBusinessDate(today);
        order.setAcceptedAt(acceptedAt);
        order.setOrderSequence(sequences.generateOrderNumber(today));
        order.setSubtotal(quote.subtotal());
        order.setPromoDiscount(quote.promoDiscount());
        order.setPointsDiscount(quote.pointsDiscount());
        order.setPointsRedeemed(quote.pointsRedeemed());
        order.setPromoCode(code.isEmpty() ? null : code);
        order.setTotalAmount(quote.totalAmount());
        order.setLoyaltyPoint(quote.pointsEarned());
        Orders saved = orders.save(order);
        rewards.reserve(saved, kiosk);
        return saved;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckoutQuote quote(OrderRequest request) {
        Orders order = assemble(request.getItem().stream().map(CheckoutLine::of).toList());
        return price(order, customers.findByNormalizedPhone(request.getCustomerPhone()).orElse(null),
                Promotions.code(request.getPromoCode()), request.getPoints());
    }

    private Orders assemble(List<CheckoutLine> lines) {
        Orders order = new Orders();
        for (CheckoutLine line : lines) order.getOrderItems().add(buildItem(order, line));
        return order;
    }

    private CheckoutQuote price(Orders order, Customer customer, String code, int points) {
        BigDecimal subtotal = order.getOrderItems().stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.compareTo(MAX_TOTAL) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The order total is too high.");
        }
        BigDecimal promo = promotions.discount(code, order.getOrderItems());
        int available = rewards.available(customer);
        if (points > available) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You don't have enough loyalty points.");
        }
        if (points % OrderRewards.POINTS_PER_RINGGIT != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redeem points in multiples of 10.");
        }
        BigDecimal redemption = BigDecimal.valueOf(points / OrderRewards.POINTS_PER_RINGGIT);
        BigDecimal total = subtotal.subtract(promo).subtract(redemption);
        if (total.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The redeemed points exceed the total after discounts.");
        }
        return new CheckoutQuote(subtotal, promo, redemption, total, available, points, rewards.earned(total), code);
    }

    private OrderItem buildItem(Orders order, CheckoutLine line) {
        Product product = products.findForCheckout(line.product()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found."));
        if (product.getStatus() != PRODUCTS_STATUS.PRODUCT_AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, product.getName() + " is out of stock.");
        }

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot(product.getName());
        item.setSalesUnitSnapshot(product.getSalesUnit());
        item.setPrice(product.getPrice());
        item.setQuantity(line.quantity());
        item.setNote(line.note());
        var usedCategories = new HashSet<Long>();
        for (Long variantId : line.variants()) {
            ProductVariant variant = variants.findForCheckout(variantId).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product option not found."));
            if (!product.getId().equals(variant.getProduct().getId()) || !usedCategories.add(variant.getCategory().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid option for " + product.getName() + ".");
            }
            OrderItemVariant selection = new OrderItemVariant();
            selection.setOrderItemId(item);
            selection.setProductVaraintId(variant);
            selection.setExtraPrice(variant.getExtraPrice());
            selection.setCategorySnapshot(variant.getCategory().getName());
            selection.setValueSnapshot(variant.getValue());
            item.getOrderItemVariants().add(selection);
        }
        return item;
    }

    @Override
    public List<Orders> getAllOrder() {
        return orders.findAll();
    }

    @Override
    public List<Orders> getAllOrderPendingPreparing() {
        return orders.findAllByStatusIn(List.of(ORDER_STATUS.ORDER_PREPARING, ORDER_STATUS.ORDER_PENDING));
    }

    private static String digest(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record CheckoutLine(Long product, int quantity, List<Long> variants, String note) {
        private static CheckoutLine of(OrderItemRequest item) {
            List<Long> variants = item.getVariantIds() == null ? List.of() : item.getVariantIds().stream().sorted().toList();
            String note = item.getNote() == null ? "" : item.getNote().strip();
            return new CheckoutLine(item.getProduct(), item.getQuantity(), variants, note);
        }
    }
}
