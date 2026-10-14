package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

public interface PromotionRule {
    Optional<DiscountLine> apply(ListContext context);
    int priority();

    record ListContext(java.util.List<CartLine> lines, PaymentMethod paymentMethod, BigDecimal currentTotal) { }
}
