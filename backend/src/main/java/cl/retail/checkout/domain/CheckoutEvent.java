package cl.retail.checkout.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckoutEvent(String eventId, String eventType, Instant occurredAt, String orderId,
                            BigDecimal subtotal, BigDecimal productDiscount, BigDecimal cartDiscount,
                            BigDecimal paymentDiscount, BigDecimal finalTotal, String paymentMethod,
                            List<String> promotionCodes, int schemaVersion) { }
