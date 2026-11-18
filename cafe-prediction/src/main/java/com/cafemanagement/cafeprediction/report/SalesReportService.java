package com.cafemanagement.cafeprediction.report;

import com.cafemanagement.cafeprediction.model.enumoption.SALES_UNIT;
import com.cafemanagement.cafeprediction.repository.OrderPaymentRepository;
import com.cafemanagement.cafeprediction.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class SalesReportService {
    private final OrderPaymentRepository payments;
    private final OrderRepository orders;
    private final ZoneId zone;

    public SalesReportService(OrderPaymentRepository payments, OrderRepository orders,
                              @Value("${cafe.business-zone}") String businessZone) {
        this.payments = payments;
        this.orders = orders;
        this.zone = ZoneId.of(businessZone);
    }

    public Report report(LocalDate from, LocalDate to, Group group) {
        Range range = range(from, to);
        var buckets = new LinkedHashMap<LocalDate, Totals>();
        var totals = new Totals();
        Map<Long, ProductCount> products = new HashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            buckets.computeIfAbsent(group.start(day), ignored -> new Totals());
        }

        for (var payment : payments.salesPayments(range.start(), range.end())) {
            if (range.contains(payment.paidAt())) {
                totals.paid(payment);
                buckets.get(group.start(date(payment.paidAt()))).paid(payment);
            }
            if (range.contains(payment.refundedAt())) {
                totals.refund(payment);
                buckets.get(group.start(date(payment.refundedAt()))).refund(payment);
            }
        }
        for (Instant cancelled : orders.salesCancellations(range.start(), range.end())) {
            totals.cancelled++;
            buckets.get(group.start(date(cancelled))).cancelled++;
        }

        for (var item : payments.salesItems(range.start(), range.end())) {
            ProductCount product = products.computeIfAbsent(item.productId(), ignored -> new ProductCount(item));
            if (range.contains(item.paidAt())) {
                totals.add(item.unit(), item.quantity());
                buckets.get(group.start(date(item.paidAt()))).add(item.unit(), item.quantity());
                product.paid += item.quantity();
            }
            if (range.contains(item.refundedAt())) {
                totals.add(item.unit(), -item.quantity());
                buckets.get(group.start(date(item.refundedAt()))).add(item.unit(), -item.quantity());
                product.refunded += item.quantity();
            }
        }

        List<Period> periods = buckets.entrySet().stream().map(entry -> {
            LocalDate start = entry.getKey();
            LocalDate end = group.end(start);
            return new Period(start.isBefore(from) ? from : start, end.isAfter(to) ? to : end, entry.getValue().view());
        }).toList();
        List<Product> ranking = products.entrySet().stream()
                .map(entry -> entry.getValue().view(entry.getKey()))
                .sorted(Comparator.comparingLong(Product::netQuantity).reversed()
                        .thenComparing(Comparator.comparingLong(Product::paidQuantity).reversed())
                        .thenComparing(Product::productId)).toList();
        return new Report(from, to, zone.getId(), group, totals.view(), periods, ranking);
    }

    public ProductHistory productHistory(LocalDate from, LocalDate to) {
        Range range = range(from, to);
        Map<ProductDay, ProductCount> quantities = new HashMap<>();
        for (var item : payments.salesItems(range.start(), range.end())) {
            if (range.contains(item.paidAt())) {
                var key = new ProductDay(date(item.paidAt()), item.productId());
                quantities.computeIfAbsent(key, ignored -> new ProductCount(item)).paid += item.quantity();
            }
            if (range.contains(item.refundedAt())) {
                var key = new ProductDay(date(item.refundedAt()), item.productId());
                quantities.computeIfAbsent(key, ignored -> new ProductCount(item)).refunded += item.quantity();
            }
        }
        List<DailyProduct> daily = quantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ProductDay::date).thenComparing(ProductDay::productId)))
                .map(entry -> {
                    Product product = entry.getValue().view(entry.getKey().productId());
                    return new DailyProduct(entry.getKey().date(), product.productId(), product.productName(),
                            product.salesUnit(), product.paidQuantity(), product.refundedQuantity(), product.netQuantity());
                }).toList();
        return new ProductHistory(from, to, zone.getId(), daily);
    }

    private LocalDate date(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }

    private Range range(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to);
        if (days < 0 || days > 365 || from.getYear() < 1000 || to.getYear() > 9998) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid date range of up to 366 days.");
        }
        return new Range(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private record Range(Instant start, Instant end) {
        boolean contains(Instant instant) {
            return instant != null && !instant.isBefore(start) && instant.isBefore(end);
        }
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final class Totals {
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal discounts = BigDecimal.ZERO;
        private BigDecimal collected = BigDecimal.ZERO;
        private BigDecimal refunded = BigDecimal.ZERO;
        private long paidOrders;
        private long refundOrders;
        private long cancelled;
        private final Map<SALES_UNIT, Long> units = new EnumMap<>(SALES_UNIT.class);

        void paid(SalesEvents.Payment payment) {
            BigDecimal discount = zeroIfNull(payment.promoDiscount()).add(zeroIfNull(payment.pointsDiscount()));
            gross = gross.add(payment.subtotal() == null ? payment.amount().add(discount) : payment.subtotal());
            discounts = discounts.add(discount);
            collected = collected.add(payment.amount());
            paidOrders++;
        }

        void refund(SalesEvents.Payment payment) {
            refunded = refunded.add(payment.amount());
            refundOrders++;
        }

        void add(SALES_UNIT unit, long change) {
            units.merge(unit, change, Long::sum);
        }

        Amounts view() {
            return new Amounts(gross, discounts, collected, refunded, collected.subtract(refunded),
                    paidOrders, refundOrders, cancelled, units.getOrDefault(SALES_UNIT.CUP, 0L),
                    units.getOrDefault(SALES_UNIT.FOOD, 0L), units.getOrDefault(SALES_UNIT.OTHER, 0L),
                    units.getOrDefault(SALES_UNIT.UNCLASSIFIED, 0L));
        }
    }

    private static final class ProductCount {
        private final String name;
        private final SALES_UNIT unit;
        private long paid;
        private long refunded;

        ProductCount(SalesEvents.Item item) {
            this.name = item.productName();
            this.unit = item.currentUnit() == null ? SALES_UNIT.UNCLASSIFIED : item.currentUnit();
        }

        Product view(long id) {
            return new Product(id, name, unit, paid, refunded, paid - refunded);
        }
    }

    private record ProductDay(LocalDate date, Long productId) {}

    public enum Group {
        DAY, WEEK, MONTH;

        LocalDate start(LocalDate date) {
            return switch (this) {
                case DAY -> date;
                case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case MONTH -> date.withDayOfMonth(1);
            };
        }

        LocalDate end(LocalDate start) {
            return switch (this) {
                case DAY -> start;
                case WEEK -> start.plusDays(6);
                case MONTH -> start.with(TemporalAdjusters.lastDayOfMonth());
            };
        }
    }

    public record Amounts(BigDecimal gross, BigDecimal discounts, BigDecimal collected, BigDecimal refunded, BigDecimal net,
                          long paidOrders, long refundedOrders, long cancelledOrders,
                          long cups, long food, long other, long unclassified) {}
    public record Period(LocalDate from, LocalDate to, Amounts amounts) {}
    public record Product(Long productId, String productName, SALES_UNIT salesUnit,
                          long paidQuantity, long refundedQuantity, long netQuantity) {}
    public record Report(LocalDate from, LocalDate to, String zone, Group group, Amounts totals,
                         List<Period> periods, List<Product> products) {}
    public record DailyProduct(LocalDate date, Long productId, String productName, SALES_UNIT salesUnit,
                               long paidQuantity, long refundedQuantity, long netQuantity) {}
    public record ProductHistory(LocalDate from, LocalDate to, String zone, List<DailyProduct> rows) {}
}
