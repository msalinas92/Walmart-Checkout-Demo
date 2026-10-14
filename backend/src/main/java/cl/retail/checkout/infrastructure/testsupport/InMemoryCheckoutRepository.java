package cl.retail.checkout.infrastructure.testsupport;

import cl.retail.checkout.application.CheckoutRepository;
import cl.retail.checkout.domain.CheckoutOrder;
import cl.retail.checkout.domain.Product;
import cl.retail.checkout.domain.Quote;
import cl.retail.checkout.domain.PromotionDefinition;
import cl.retail.checkout.domain.PaymentMethod;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCheckoutRepository implements CheckoutRepository {
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, Quote> quotes = new ConcurrentHashMap<>();
    private final Map<String, CheckoutOrder> orders = new ConcurrentHashMap<>();
    private final Map<String, String> idempotency = new ConcurrentHashMap<>();
    private final Set<String> pendingEvents = ConcurrentHashMap.newKeySet();

    public InMemoryCheckoutRepository(Collection<Product> seed) { seed.forEach(product -> products.put(product.id(), product)); }
    @Override public List<Product> findProducts(List<String> ids) { return ids.stream().distinct().map(products::get).filter(Objects::nonNull).toList(); }
    @Override public List<Product> searchProducts(String query, String category) { String term = query == null ? "" : query.toLowerCase(); return products.values().stream().filter(product -> (category == null || category.isBlank() || product.category().equalsIgnoreCase(category)) && (term.isBlank() || product.name().toLowerCase().contains(term))).toList(); }
    @Override public List<PromotionDefinition> findActivePromotions() { return List.of(
            new PromotionDefinition("CART_5_OVER_200K", "5% por compras sobre $200.000", "CART_THRESHOLD", java.math.BigDecimal.valueOf(5), java.math.BigDecimal.valueOf(200000), java.math.BigDecimal.ZERO, null, 200, true),
            new PromotionDefinition("DEBIT_10", "10% de descuento pagando con débito", "PAYMENT_METHOD", java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(50000), PaymentMethod.DEBIT, 300, true)); }
    @Override public void saveQuote(Quote quote) { quotes.put(quote.id(), quote); }
    @Override public Optional<Quote> findQuote(String id) { return Optional.ofNullable(quotes.get(id)); }
    @Override public void saveOrder(CheckoutOrder order, String key) { orders.put(order.id(), order); idempotency.put(key, order.id()); pendingEvents.add(order.id()); }
    @Override public boolean isEventPending(String orderId) { return pendingEvents.contains(orderId); }
    @Override public void markEventPublished(String orderId) { pendingEvents.remove(orderId); }
    @Override public Optional<CheckoutOrder> findOrderByIdempotencyKey(String key) { return Optional.ofNullable(idempotency.get(key)).flatMap(this::findOrder); }
    @Override public Optional<CheckoutOrder> findOrder(String id) { return Optional.ofNullable(orders.get(id)); }
}
