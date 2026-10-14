package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PricingEngine {
    private final List<PromotionRule> rules;

    public PricingEngine(List<PromotionRule> rules) {
        this.rules = rules.stream().sorted(Comparator.comparingInt(PromotionRule::priority)).toList();
    }

    public Quote quote(List<CartLine> lines, PaymentMethod paymentMethod) {
        BigDecimal subtotal = Money.pesos(lines.stream().map(CartLine::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal runningTotal = subtotal;
        List<DiscountLine> applied = new ArrayList<>();
        for (PromotionRule rule : rules) {
            // Aquí aplico una regla sobre el total ya descontado para mantener un cálculo determinista.
            var discount = rule.apply(new PromotionRule.ListContext(lines, paymentMethod, runningTotal));
            if (discount.isPresent()) {
                applied.add(discount.get());
                runningTotal = Money.pesos(runningTotal.subtract(discount.get().amount()).max(BigDecimal.ZERO));
            }
        }
        return new Quote("quo_" + UUID.randomUUID(), List.copyOf(lines), paymentMethod, subtotal,
                List.copyOf(applied), BigDecimal.ZERO, runningTotal,
                Instant.now().plus(5, ChronoUnit.MINUTES), "v1");
    }
}
