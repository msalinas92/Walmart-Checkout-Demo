package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Quote(String id, List<CartLine> lines, PaymentMethod paymentMethod, BigDecimal subtotal,
                    List<DiscountLine> discounts, BigDecimal shippingCost, BigDecimal total,
                    Instant expiresAt, String promotionVersion) { }
