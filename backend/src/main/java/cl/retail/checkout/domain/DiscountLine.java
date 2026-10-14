package cl.retail.checkout.domain;

import java.math.BigDecimal;

public record DiscountLine(String code, String name, DiscountType type, BigDecimal amount,
                           BigDecimal calculationBase, int priority, boolean stackable) { }
