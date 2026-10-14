package cl.retail.checkout.application;

import cl.retail.checkout.domain.CheckoutOrder;
import cl.retail.checkout.domain.Product;
import cl.retail.checkout.domain.PromotionDefinition;
import cl.retail.checkout.domain.Quote;
import java.util.List;
import java.util.Optional;

public interface CheckoutRepository {
    List<Product> findProducts(List<String> productIds);
    List<Product> searchProducts(String query, String category);
    List<PromotionDefinition> findActivePromotions();
    void saveQuote(Quote quote);
    Optional<Quote> findQuote(String quoteId);
    void saveOrder(CheckoutOrder order, String idempotencyKey);
    boolean isEventPending(String orderId);
    void markEventPublished(String orderId);
    Optional<CheckoutOrder> findOrderByIdempotencyKey(String idempotencyKey);
    Optional<CheckoutOrder> findOrder(String orderId);
}
