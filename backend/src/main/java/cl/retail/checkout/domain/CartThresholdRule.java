package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

public final class CartThresholdRule implements PromotionRule {
    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(200_000);
    @Override
    public Optional<DiscountLine> apply(ListContext context) {
        if (context.currentTotal().compareTo(THRESHOLD) < 0) return Optional.empty();
        BigDecimal amount = Money.percentage(context.currentTotal(), BigDecimal.valueOf(5));
        return Optional.of(new DiscountLine("CART_5_OVER_200K", "5% por compras sobre $200.000",
                DiscountType.CART, amount, context.currentTotal(), priority(), true));
    }
    @Override public int priority() { return 200; }
}
