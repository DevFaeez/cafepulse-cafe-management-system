package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.model.Stock;
import com.cafemanagement.cafeprediction.model.StockMovement;
import com.cafemanagement.cafeprediction.model.StockMovement.Kind;
import com.cafemanagement.cafeprediction.model.enumoption.STOCK_STATUS;
import com.cafemanagement.cafeprediction.repository.StockMovementRepository;
import com.cafemanagement.cafeprediction.repository.StockRepository;
import com.cafemanagement.cafeprediction.request.CreateNewStockRequest;
import com.cafemanagement.cafeprediction.request.StockMovementRequest;
import com.cafemanagement.cafeprediction.request.StockMovementRequest.Type;
import com.cafemanagement.cafeprediction.response.StockMovementView;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class StockServiceImpl implements StockService {
    private static final BigDecimal MAX_BALANCE = new BigDecimal("999999999.999");
    private final StockRepository stocks;
    private final StockMovementRepository movements;

    public StockServiceImpl(StockRepository stocks, StockMovementRepository movements) {
        this.stocks = stocks;
        this.movements = movements;
    }

    @Override
    @Transactional
    public Stock createNewStock(CreateNewStockRequest request, String actor) {
        Stock stock = new Stock();
        stock.setStockName(request.stockName().strip());
        stock.setStatus(STOCK_STATUS.STOCK_OUT_OF_STOCK);
        stocks.save(stock);
        if (request.unit() != null || request.openingQuantity() != null || request.minimum() != null) {
            configureOpening(stock, request, actor);
        }
        return stock;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Stock settings(Long id, CreateNewStockRequest request, String actor) {
        Stock stock = lock(id);
        stock.setStockName(request.stockName().strip());
        if (!stock.isConfigured()) {
            configureOpening(stock, request, actor);
        } else {
            if (request.openingQuantity() != null || request.unit() != stock.getUnit() || request.minimum() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Keep the original unit and record a stock movement to change the balance.");
            }
            stock.setMinimum(request.minimum());
            stock.updateStatus();
        }
        return stock;
    }

    private void configureOpening(Stock stock, CreateNewStockRequest request, String actor) {
        if (request.unit() == null || request.minimum() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a stock unit and minimum level.");
        }
        if (stock.getUnit() != null && stock.getUnit() != request.unit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keep the stock unit used by recipes.");
        }
        stock.setUnit(request.unit());
        stock.setMinimum(request.minimum());
        if (request.openingQuantity() != null) {
            stock.setQuantity(BigDecimal.ZERO);
            post(stock, null, Kind.OPENING, request.openingQuantity(), null, null, "Opening count", actor);
        }
    }

    @Override
    @Transactional
    public Stock updateStockStatus(Long id, STOCK_STATUS status) {
        Stock stock = lock(id);
        if (stock.isConfigured()) {
            throw conflict("The ingredient status is calculated from its stock balance and minimum level.");
        }
        if (status == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a status.");
        stock.setStatus(status);
        return stock;
    }

    @Override
    public List<Stock> getAllStock() {
        return stocks.findAllByOrderByStockNameAscIdAsc();
    }

    @Override
    public List<Stock> alerts() {
        return stocks.findAllByQuantityIsNotNullAndStatusInOrderByStockNameAsc(
                List.of(STOCK_STATUS.STOCK_OUT_OF_STOCK, STOCK_STATUS.STOCK_LOW_STOCK));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StockMovementView move(Long id, UUID key, StockMovementRequest request, String actor) {
        Stock stock = lock(id);
        requireConfigured(stock);
        String reason = request.reason().strip();
        Kind kind = Kind.valueOf(request.type().name());
        var previous = movements.findByStockIdAndRequestId(id, key.toString());
        if (previous.isPresent()) {
            StockMovement entry = previous.get();
            if (entry.getKind() != kind || entry.getRequestedQuantity().compareTo(request.quantity()) != 0
                    || !Objects.equals(entry.getReason(), reason) || !Objects.equals(entry.getActor(), actor)) {
                throw conflict("This submission ID was already used for a different stock movement.");
            }
            return StockMovementView.of(entry);
        }

        BigDecimal quantity = request.quantity();
        if (request.type() == Type.ADJUSTMENT) {
            if (!Objects.equals(request.revision(), stock.getRevision())) {
                throw conflict("The balance has changed. Check it and recount before making an adjustment.");
            }
        } else if (quantity.signum() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a quantity greater than zero.");
        }
        BigDecimal delta = switch (request.type()) {
            case ADJUSTMENT -> quantity.subtract(stock.getQuantity());
            case RECEIPT -> quantity;
            case USAGE, WASTE -> quantity.negate();
        };
        return StockMovementView.of(post(stock, null, kind, delta, quantity, key.toString(), reason, actor));
    }

    @Override
    @Transactional(readOnly = true)
    public StockMovementView.History history(Long id, int page) {
        if (!stocks.existsById(id)) throw missing();
        var entries = movements.findByStockIdOrderByIdDesc(id, PageRequest.of(page, 20));
        return new StockMovementView.History(entries.stream().map(StockMovementView::of).toList(), entries.hasNext());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deduct(Orders order, Map<Long, BigDecimal> amounts, String actor) {
        for (var entry : new TreeMap<>(amounts).entrySet()) {
            Stock stock = lock(entry.getKey());
            requireConfigured(stock);
            post(stock, order, Kind.ORDER_USAGE, entry.getValue().negate(), null, null, "Order preparation", actor);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restore(Orders order, String actor) {
        Map<Long, BigDecimal> amounts = new TreeMap<>();
        for (StockMovement movement : movements.findAllByOrderIdAndKind(order.getId(), Kind.ORDER_USAGE)) {
            amounts.put(movement.getStock().getId(), movement.getDelta().negate());
        }
        if (amounts.isEmpty()) throw conflict("No recipe deductions are available to return.");
        for (var entry : amounts.entrySet()) {
            post(lock(entry.getKey()), order, Kind.ORDER_RETURN, entry.getValue(), null, null, "Unused ingredients returned", actor);
        }
    }

    private StockMovement post(Stock stock, Orders order, Kind kind, BigDecimal delta, BigDecimal requested,
                               String key, String reason, String actor) {
        BigDecimal after = stock.getQuantity().add(delta);
        if (after.signum() < 0) throw conflict("Not enough stock for " + stock.getStockName() + ".");
        if (after.compareTo(MAX_BALANCE) > 0) throw conflict("The stock balance exceeds the maximum allowed.");
        stock.setQuantity(after);
        stock.advanceRevision();
        stock.updateStatus();
        return movements.save(new StockMovement(stock, order, kind, delta, after, requested, key, reason, actor));
    }

    private static void requireConfigured(Stock stock) {
        if (!stock.isConfigured()) throw conflict("A manager must enter an opening count for " + stock.getStockName() + ".");
    }

    private Stock lock(Long id) {
        return stocks.lockById(id).orElseThrow(StockServiceImpl::missing);
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found.");
    }

    private static ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
