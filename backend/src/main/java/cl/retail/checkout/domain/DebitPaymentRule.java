package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

public final class DebitPaymentRule implements PromotionRule {
    private static final BigDecimal MAX_DISCOUNT = BigDecimal.valueOf(50_000);
    @Override
    public Optional<DiscountLine> apply(ListContext context) {
        if (context.paymentMethod() != PaymentMethod.DEBIT) return Optional.empty();
        BigDecimal discount = Money.percentage(context.currentTotal(), BigDecimal.TEN).min(MAX_DISCOUNT);
        return Optional.of(new DiscountLine("DEBIT_10", "10% de descuento pagando con débito",
                DiscountType.PAYMENT_METHOD, discount, context.currentTotal(), priority(), false));
    }
    @Override public int priority() { return 300; }
}
