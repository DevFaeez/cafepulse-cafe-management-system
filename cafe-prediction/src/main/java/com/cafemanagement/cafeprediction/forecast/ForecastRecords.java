package com.cafemanagement.cafeprediction.forecast;

import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import com.cafemanagement.cafeprediction.model.enumoption.SALES_UNIT;
import com.cafemanagement.cafeprediction.report.SalesReportService;
import com.cafemanagement.cafeprediction.repository.ProductRepository;
import com.cafemanagement.cafeprediction.service.DailyOrderSequenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ForecastRecords {
    private final ForecastProductRepository mappings;
    private final DailyForecastObservationRepository history;
    private final ProductRepository products;
    private final SalesReportService sales;
    private final DailyOrderSequenceService checkout;
    private final ObjectMapper json;
    private final ZoneId zone;

    public ForecastRecords(ForecastProductRepository mappings, DailyForecastObservationRepository history,
                           ProductRepository products, SalesReportService sales, DailyOrderSequenceService checkout,
                           ObjectMapper json, @Value("${cafe.business-zone}") String zone) {
        this.mappings = mappings;
        this.history = history;
        this.products = products;
        this.sales = sales;
        this.checkout = checkout;
        this.json = json;
        this.zone = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public List<Mapping> mappings() {
        Map<Drink, Product> configured = mappings.findAll().stream()
                .collect(Collectors.toMap(ForecastProduct::getDrink, ForecastProduct::getProduct));
        return Arrays.stream(Drink.values()).map(drink -> {
            Product product = configured.get(drink);
            return new Mapping(drink, drink.label(), product == null ? null : product.getId(),
                    product == null ? null : product.getName(),
                    product != null && product.getStatus() == PRODUCTS_STATUS.PRODUCT_AVAILABLE && product.getSalesUnit() == SALES_UNIT.CUP);
        }).toList();
    }

    @Transactional
    public void map(Drink drink, Long productId) {
        checkout.lockCheckout();
        if (productId == null) {
            mappings.deleteById(drink);
            return;
        }
        Product product = products.findById(productId).filter(item -> item.getSalesUnit() == SALES_UNIT.CUP)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a product with Cups as its sales unit."));
        try {
            mappings.saveAndFlush(new ForecastProduct(drink, product));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This product is already mapped to another drink.");
        }
    }

    @Transactional(readOnly = true)
    public Observation get(LocalDate date) {
        return history.findById(date).map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No observation has been saved for this date."));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Observation record(ObservationInput input, String actor) {
        LocalDate date = input.scenario().date();
        if (date.getYear() < 1000 || date.isAfter(LocalDate.now(zone))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid observation date.");
        }
        checkout.lockCheckout();
        var prior = history.findById(date).map(this::view);
        List<Long> tracked = prior.map(observation -> observation.products().stream().map(Tracked::productId).toList())
                .orElseGet(() -> mappings.findAll().stream().map(mapping -> mapping.getProduct().getId()).sorted().toList());
        if (!tracked.containsAll(input.unavailableProductIds())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select only the products tracked for this date.");
        }
        List<Tracked> conditions = tracked.stream()
                .map(id -> new Tracked(id, input.unavailableProductIds().contains(id))).toList();
        return view(history.save(new DailyForecastObservation(date, encode(input.scenario()), encode(conditions), input.closed(), actor)));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TrainingData training(LocalDate from, LocalDate to) {
        var quantities = sales.productHistory(from, to);
        var indexed = quantities.rows().stream().collect(Collectors.toMap(
                row -> new SaleKey(row.date(), row.productId()), row -> row));
        var rows = history.findByBusinessDateBetweenOrderByBusinessDateAsc(from, to).stream().map(this::view)
                .map(observation -> new TrainingDay(observation.scenario().date(), observation.scenario().features(), observation.closed(),
                        observation.products().stream().map(product -> {
                            var quantity = indexed.get(new SaleKey(observation.scenario().date(), product.productId()));
                            return new TrainingProduct(product.productId(), product.unavailable(),
                                    quantity == null ? 0 : quantity.paidQuantity(), quantity == null ? 0 : quantity.refundedQuantity());
                        }).toList())).toList();
        return new TrainingData(from, to, zone.getId(), rows);
    }

    private Observation view(DailyForecastObservation record) {
        try {
            return new Observation(json.readValue(record.getScenarioJson(), ForecastScenario.class), record.isClosed(),
                    json.readValue(record.getProductsJson(), new TypeReference<List<Tracked>>() {}),
                    record.getUpdatedAt(), record.getUpdatedBy());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored observation is invalid", ex);
        }
    }

    private String encode(Object data) {
        try {
            return json.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize observation", ex);
        }
    }

    private record SaleKey(LocalDate date, long productId) {}
    public record Mapping(Drink drink, String label, Long productId, String productName, boolean available) {}
    public record ObservationInput(@NotNull @Valid ForecastScenario scenario, @NotNull Boolean closed,
                                   @NotNull @Size(max = 8) Set<@Positive @NotNull Long> unavailableProductIds) {}
    public record Tracked(long productId, boolean unavailable) {}
    public record Observation(ForecastScenario scenario, boolean closed, List<Tracked> products, Instant updatedAt, String updatedBy) {}
    public record TrainingProduct(long productId, boolean unavailable, long paidQuantity, long refundedQuantity) {}
    public record TrainingDay(LocalDate date, Map<String, Object> features, boolean closed, List<TrainingProduct> products) {}
    public record TrainingData(LocalDate from, LocalDate to, String zone, List<TrainingDay> rows) {}
}
