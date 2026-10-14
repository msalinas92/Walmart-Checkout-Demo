package cl.retail.checkout.domain;

import java.time.Instant;

public record CheckoutOrder(String id, Quote quote, OrderStatus status, Instant confirmedAt) { }
