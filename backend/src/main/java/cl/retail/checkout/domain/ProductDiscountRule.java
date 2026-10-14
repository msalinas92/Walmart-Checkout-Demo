package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

public final class ProductDiscountRule implements PromotionRule {
    @Override
    public Optional<DiscountLine> apply(ListContext context) {
        BigDecimal base = context.lines().stream()
                .filter(line -> line.product().productDiscountPercent().signum() > 0)
                .map(CartLine::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = context.lines().stream()
                .map(line -> Money.percentage(line.grossAmount(), line.product().productDiscountPercent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return discount.signum() == 0 ? Optional.empty() : Optional.of(new DiscountLine(
                "PRODUCT_OFFER", "Ofertas en productos seleccionados", DiscountType.PRODUCT,
                Money.pesos(discount), Money.pesos(base), priority(), true));
    }

    @Override public int priority() { return 100; }
}
