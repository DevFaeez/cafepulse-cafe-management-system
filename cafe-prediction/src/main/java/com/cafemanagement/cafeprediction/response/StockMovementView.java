package com.cafemanagement.cafeprediction.response;

import com.cafemanagement.cafeprediction.model.StockMovement;
import com.cafemanagement.cafeprediction.model.StockMovement.Kind;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StockMovementView(Long id, Kind kind, BigDecimal delta, BigDecimal balanceAfter, String reason,
                                String actor, Instant recordedAt, String orderSequence) {
    public static StockMovementView of(StockMovement entry) {
        return new StockMovementView(entry.getId(), entry.getKind(), entry.getDelta(), entry.getBalanceAfter(),
                entry.getReason(), entry.getActor(), entry.getRecordedAt(),
                entry.getOrder() == null ? null : entry.getOrder().getOrderSequence());
    }

    public record History(List<StockMovementView> movements, boolean hasNext) {}
}
