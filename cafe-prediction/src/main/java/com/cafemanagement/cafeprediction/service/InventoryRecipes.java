package com.cafemanagement.cafeprediction.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.model.RecipeLine;
import com.cafemanagement.cafeprediction.model.RecipeLine.Owner;
import com.cafemanagement.cafeprediction.model.Stock;
import com.cafemanagement.cafeprediction.repository.ProductRepository;
import com.cafemanagement.cafeprediction.repository.ProductVariantRepository;
import com.cafemanagement.cafeprediction.repository.RecipeLineRepository;
import com.cafemanagement.cafeprediction.repository.StockRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class InventoryRecipes {
    private final RecipeLineRepository recipes;
    private final StockRepository stocks;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final StockServiceImpl inventory;
    private final DailyOrderSequenceService checkout;

    public InventoryRecipes(RecipeLineRepository recipes, StockRepository stocks, ProductRepository products,
                            ProductVariantRepository variants, StockServiceImpl inventory, DailyOrderSequenceService checkout) {
        this.recipes = recipes;
        this.stocks = stocks;
        this.products = products;
        this.variants = variants;
        this.inventory = inventory;
        this.checkout = checkout;
    }

    @Transactional(readOnly = true)
    public List<Ingredient> get(Owner type, long id) {
        requireOwner(type, id);
        return recipes.findByOwnerTypeAndOwnerIdOrderById(type, id).stream()
                .map(line -> new Ingredient(line.getStock().getId(), line.getReplacementStock() == null ? line.getQuantity() : null,
                        line.getReplacementStock() == null ? null : line.getReplacementStock().getId())).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Ingredient> replace(Owner type, long id, Input input) {
        checkout.lockCheckout();
        requireOwner(type, id);
        List<Long> sourceIds = input.ingredients().stream().map(Ingredient::stockId).distinct().toList();
        if (sourceIds.size() != input.ingredients().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use one line per ingredient.");
        }
        Set<Long> stockIds = input.ingredients().stream()
                .flatMap(ingredient -> Stream.concat(Stream.of(ingredient.stockId()), Stream.ofNullable(ingredient.replacementStockId())))
                .collect(Collectors.toSet());
        Map<Long, Stock> byId = stocks.findAllById(stockIds).stream()
                .filter(stock -> stock.getUnit() != null)
                .collect(Collectors.toMap(Stock::getId, stock -> stock));
        if (byId.size() != stockIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Set a stock unit for every ingredient in the recipe.");
        }

        List<RecipeLine> lines = input.ingredients().stream()
                .map(ingredient -> recipeLine(type, id, ingredient, byId)).toList();
        recipes.removeRecipe(type, id);
        recipes.saveAll(lines);
        return input.ingredients();
    }

    private RecipeLine recipeLine(Owner type, long id, Ingredient ingredient, Map<Long, Stock> stocksById) {
        Stock source = stocksById.get(ingredient.stockId());
        Long targetId = ingredient.replacementStockId();
        Stock target = targetId == null ? null : stocksById.get(targetId);
        if (target == null && ingredient.quantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a quantity for each ingredient added.");
        }
        if (target != null && (type != Owner.VARIANT || ingredient.quantity() != null
                || source.getId().equals(targetId) || source.getUnit() != target.getUnit())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Replace ingredients only in option recipes and choose a different ingredient with the same stock unit.");
        }
        return new RecipeLine(type, id, source, ingredient.quantity(), target);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deduct(Orders order, String actor) {
        Set<Long> productIds = order.getOrderItems().stream().map(item -> item.getProduct().getId()).collect(Collectors.toSet());
        Set<Long> optionIds = order.getOrderItems().stream().flatMap(item -> item.getOrderItemVariants().stream())
                .map(option -> option.getProductVaraintId().getId()).collect(Collectors.toSet());
        Map<Long, List<RecipeLine>> baseRecipes = load(Owner.PRODUCT, productIds);
        Map<Long, List<RecipeLine>> optionRecipes = load(Owner.VARIANT, optionIds);
        Map<Long, BigDecimal> amounts = new HashMap<>();
        for (OrderItem item : order.getOrderItems()) {
            Map<Long, BigDecimal> perUnit = new HashMap<>();
            Map<Long, Long> replacements = new HashMap<>();
            for (RecipeLine line : baseRecipes.getOrDefault(item.getProduct().getId(), List.of())) {
                addLine(line, perUnit, replacements);
            }
            for (var option : item.getOrderItemVariants()) {
                for (RecipeLine line : optionRecipes.getOrDefault(option.getProductVaraintId().getId(), List.of())) {
                    addLine(line, perUnit, replacements);
                }
            }
            for (var ingredient : perUnit.entrySet()) {
                Long actual = replacements.getOrDefault(ingredient.getKey(), ingredient.getKey());
                BigDecimal amount = ingredient.getValue().multiply(BigDecimal.valueOf(item.getQuantity()));
                amounts.merge(actual, amount, BigDecimal::add);
            }
        }
        inventory.deduct(order, amounts, actor);
        order.setRecipeStockDeducted(!amounts.isEmpty());
    }

    private Map<Long, List<RecipeLine>> load(Owner type, Set<Long> ids) {
        return ids.isEmpty() ? Map.of() : recipes.findByOwnerTypeAndOwnerIdIn(type, ids).stream()
                .collect(Collectors.groupingBy(RecipeLine::getOwnerId));
    }

    private void addLine(RecipeLine line, Map<Long, BigDecimal> amounts, Map<Long, Long> replacements) {
        Long source = line.getStock().getId();
        if (line.getReplacementStock() == null) {
            amounts.merge(source, line.getQuantity(), BigDecimal::add);
            return;
        }
        Long target = line.getReplacementStock().getId();
        Long prior = replacements.putIfAbsent(source, target);
        if (prior != null && !prior.equals(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The selected options replace the same ingredient with different ingredients.");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restore(Orders order, String actor) {
        inventory.restore(order, actor);
    }

    private void requireOwner(Owner type, long id) {
        boolean exists = type == Owner.PRODUCT ? products.existsById(id) : variants.existsById(id);
        if (!exists) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The product or option for this recipe could not be found.");
    }

    public record Ingredient(@NotNull @Positive Long stockId,
                             @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantity,
                             @JsonInclude(JsonInclude.Include.NON_NULL) @Positive Long replacementStockId) {}
    public record Input(@NotNull @Size(max = 100) List<@NotNull @Valid Ingredient> ingredients) {}
}
