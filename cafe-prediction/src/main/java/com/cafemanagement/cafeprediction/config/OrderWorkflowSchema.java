package com.cafemanagement.cafeprediction.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@DependsOn("entityManagerFactory")
class OrderWorkflowSchema {
    private final JdbcTemplate jdbc;

    OrderWorkflowSchema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void upgradeLegacyEnums() {
        if (isNativeEnum("orders", "status")) {
            jdbc.execute("ALTER TABLE orders MODIFY COLUMN status VARCHAR(30) NOT NULL");
        }
        if (isNativeEnum("order_payment", "method")) {
            jdbc.execute("ALTER TABLE order_payment MODIFY COLUMN method VARCHAR(12) NOT NULL");
        }
        allowZeroEarnedPoints();
    }

    private void allowZeroEarnedPoints() {
        var mandatoryPositive = legacyPositiveConstraint();
        mandatoryPositive.ifPresent(check -> {
            try {
                jdbc.execute("ALTER TABLE orders DROP CHECK `" + check.name().replace("`", "``")
                        + "`, ADD CONSTRAINT cafe_orders_loyalty_nonnegative CHECK (loyalty_point >= 0)");
            } catch (DataAccessException ex) {
                if (legacyPositiveConstraint().isPresent()) throw ex;
            }
        });
    }

    private java.util.Optional<Check> legacyPositiveConstraint() {
        return jdbc.query("""
                SELECT t.constraint_name, c.check_clause FROM information_schema.table_constraints t
                JOIN information_schema.check_constraints c ON c.constraint_name = t.constraint_name
                    AND c.constraint_schema = t.constraint_schema
                WHERE t.table_schema = DATABASE() AND t.table_name = 'orders' AND t.constraint_type = 'CHECK'
                """, (row, index) -> new Check(row.getString(1), row.getString(2)))
                .stream().filter(check -> check.clause().replaceAll("[\\s()`]", "").equals("loyalty_point>=1")).findFirst();
    }

    private record Check(String name, String clause) {}

    private boolean isNativeEnum(String table, String column) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? AND data_type = 'enum'
                """, Integer.class, table, column);
        return count != null && count > 0;
    }
}
