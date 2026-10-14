package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

public final class ConfiguredPromotionRule implements PromotionRule {
    private final PromotionDefinition definition;

    public ConfiguredPromotionRule(PromotionDefinition definition) { this.definition = definition; }

    @Override
    public Optional<DiscountLine> apply(ListContext context) {
        if (!definition.active()) return Optional.empty();
        if ("CART_THRESHOLD".equals(definition.type()) && context.currentTotal().compareTo(definition.minimumAmount()) < 0) return Optional.empty();
        if ("PAYMENT_METHOD".equals(definition.type()) && context.paymentMethod() != definition.paymentMethod()) return Optional.empty();
        BigDecimal amount = Money.percentage(context.currentTotal(), definition.percent());
        if (definition.maxDiscount() != null && definition.maxDiscount().signum() > 0) amount = amount.min(definition.maxDiscount());
        DiscountType discountType = "PAYMENT_METHOD".equals(definition.type()) ? DiscountType.PAYMENT_METHOD : DiscountType.CART;
        return Optional.of(new DiscountLine(definition.code(), definition.name(), discountType,
                Money.pesos(amount), context.currentTotal(), priority(), true));
    }

    @Override public int priority() { return definition.priority(); }
}
