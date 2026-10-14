package cl.retail.checkout.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PricingEngineTest {
    private final PricingEngine engine = new PricingEngine(List.of(new ProductDiscountRule(), new CartThresholdRule(), new DebitPaymentRule()));

    @Test
    void appliesProductCartAndDebitDiscountsInPriorityOrder() {
        Product tv = new Product("tv", "TV", "Tecnología", BigDecimal.valueOf(300000), BigDecimal.valueOf(10));
        Quote quote = engine.quote(List.of(new CartLine(tv, 1)), PaymentMethod.DEBIT);
        assertThat(quote.subtotal()).isEqualByComparingTo("300000");
        assertThat(quote.discounts()).extracting(DiscountLine::code).containsExactly("PRODUCT_OFFER", "CART_5_OVER_200K", "DEBIT_10");
        assertThat(quote.total()).isEqualByComparingTo("230850");
    }

    @Test
    void ignoresDebitDiscountForCreditPayment() {
        Product coffee = new Product("coffee", "Café", "Despensa", BigDecimal.valueOf(10000), BigDecimal.ZERO);
        Quote quote = engine.quote(List.of(new CartLine(coffee, 1)), PaymentMethod.CREDIT);
        assertThat(quote.discounts()).isEmpty();
        assertThat(quote.total()).isEqualByComparingTo("10000");
    }
}
