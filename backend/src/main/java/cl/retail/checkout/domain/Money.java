package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() { }

    // Aquí normalizo todos los cálculos a pesos CLP, evitando errores de punto flotante.
    public static BigDecimal pesos(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentage(BigDecimal base, BigDecimal percent) {
        return pesos(base.multiply(percent).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
    }
}
