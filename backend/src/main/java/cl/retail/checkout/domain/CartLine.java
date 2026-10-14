package cl.retail.checkout.domain;

import java.math.BigDecimal;

public record CartLine(Product product, int quantity) {
    public BigDecimal grossAmount() {
        return Money.pesos(product.unitPrice().multiply(BigDecimal.valueOf(quantity)));
    }
}
