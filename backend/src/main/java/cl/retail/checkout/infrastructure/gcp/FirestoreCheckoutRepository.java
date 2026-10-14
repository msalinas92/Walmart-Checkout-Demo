package cl.retail.checkout.infrastructure.gcp;

import cl.retail.checkout.application.CheckoutRepository;
import cl.retail.checkout.domain.*;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteBatch;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class FirestoreCheckoutRepository implements CheckoutRepository {
    private final Firestore firestore;

    public FirestoreCheckoutRepository(Firestore firestore) { this.firestore = firestore; }

    @Override
    public List<Product> findProducts(List<String> productIds) {
        return productIds.stream().distinct().map(id -> {
            try { return firestore.collection("products").document(id).get().get(); }
            catch (Exception exception) { throw new IllegalStateException("No pude consultar Firestore", exception); }
        }).filter(DocumentSnapshot::exists).map(this::toProduct).toList();
    }

    @Override
    public List<Product> searchProducts(String query, String category) {
        try {
            // Aquí leo el catálogo desde Firestore y filtro la búsqueda de demo sin exigir índices compuestos.
            String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            String selectedCategory = category == null ? "" : category.trim();
            return firestore.collection("products").get().get().getDocuments().stream()
                    .map(this::toProduct)
                    .filter(product -> selectedCategory.isBlank() || product.category().equalsIgnoreCase(selectedCategory))
                    .filter(product -> needle.isBlank() || product.name().toLowerCase(Locale.ROOT).contains(needle)
                            || product.category().toLowerCase(Locale.ROOT).contains(needle))
                    .sorted(Comparator.comparing(Product::name)).toList();
        } catch (Exception exception) { throw new IllegalStateException("No pude buscar el catálogo en Firestore", exception); }
    }

    @Override
    public List<PromotionDefinition> findActivePromotions() {
        try {
            return firestore.collection("promotions").whereEqualTo("active", true).get().get().getDocuments().stream()
                    .map(document -> new PromotionDefinition(document.getId(), document.getString("name"), document.getString("type"),
                            decimal(document.get("percent")), decimal(document.get("minimumAmount")), decimal(document.get("maxDiscount")),
                            document.getString("paymentMethod") == null ? null : PaymentMethod.valueOf(document.getString("paymentMethod")),
                            ((Number) document.get("priority")).intValue(), true)).toList();
        } catch (Exception exception) { throw new IllegalStateException("No pude leer promociones desde Firestore", exception); }
    }

    @Override
    public void saveQuote(Quote quote) {
        try { firestore.collection("quotes").document(quote.id()).set(quoteMap(quote)).get(); }
        catch (Exception exception) { throw new IllegalStateException("No pude guardar la cotización", exception); }
    }

    @Override
    public Optional<Quote> findQuote(String quoteId) {
        try {
            DocumentSnapshot document = firestore.collection("quotes").document(quoteId).get().get();
            return document.exists() ? Optional.of(toQuote(document.getData(), quoteId)) : Optional.empty();
        } catch (Exception exception) { throw new IllegalStateException("No pude leer la cotización", exception); }
    }

    @Override
    public void saveOrder(CheckoutOrder order, String idempotencyKey) {
        try {
            // Aquí escribo orden, llave idempotente y outbox en un único batch de Firestore.
            WriteBatch batch = firestore.batch();
            batch.set(firestore.collection("orders").document(order.id()), orderMap(order));
            batch.set(firestore.collection("idempotency-keys").document(idempotencyKey), Map.of("orderId", order.id()));
            batch.set(firestore.collection("outbox-events").document(order.id()), Map.of("orderId", order.id(), "status", "PENDING", "createdAt", order.confirmedAt().toString()));
            batch.commit().get();
        } catch (Exception exception) { throw new IllegalStateException("No pude confirmar la orden", exception); }
    }

    @Override
    public boolean isEventPending(String orderId) {
        try {
            DocumentSnapshot event = firestore.collection("outbox-events").document(orderId).get().get();
            return event.exists() && !"PUBLISHED".equals(event.getString("status"));
        } catch (Exception exception) { throw new IllegalStateException("No pude consultar el outbox", exception); }
    }

    @Override
    public void markEventPublished(String orderId) {
        try {
            // Aquí cierro el outbox sólo después de que Pub/Sub confirma la publicación.
            firestore.collection("outbox-events").document(orderId)
                    .set(Map.of("status", "PUBLISHED", "publishedAt", Instant.now().toString()), SetOptions.merge()).get();
        } catch (Exception exception) { throw new IllegalStateException("No pude actualizar el outbox", exception); }
    }

    @Override
    public Optional<CheckoutOrder> findOrderByIdempotencyKey(String idempotencyKey) {
        try {
            DocumentSnapshot key = firestore.collection("idempotency-keys").document(idempotencyKey).get().get();
            return key.exists() ? findOrder(key.getString("orderId")) : Optional.empty();
        } catch (Exception exception) { throw new IllegalStateException("No pude consultar idempotencia", exception); }
    }

    @Override
    public Optional<CheckoutOrder> findOrder(String orderId) {
        try {
            DocumentSnapshot document = firestore.collection("orders").document(orderId).get().get();
            if (!document.exists()) return Optional.empty();
            Map<String, Object> data = document.getData();
            return Optional.of(new CheckoutOrder(orderId, toQuote(map(data.get("quote")), String.valueOf(data.get("quoteId"))),
                    OrderStatus.valueOf(String.valueOf(data.get("status"))), Instant.parse(String.valueOf(data.get("confirmedAt")))));
        } catch (Exception exception) { throw new IllegalStateException("No pude leer la orden", exception); }
    }

    private Product toProduct(DocumentSnapshot document) {
        return new Product(document.getId(), document.getString("name"), document.getString("category"),
                decimal(document.get("unitPrice")), decimal(document.get("productDiscountPercent")));
    }

    private Map<String, Object> quoteMap(Quote quote) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentMethod", quote.paymentMethod().name()); result.put("subtotal", quote.subtotal());
        result.put("shippingCost", quote.shippingCost()); result.put("total", quote.total()); result.put("expiresAt", quote.expiresAt().toString());
        result.put("promotionVersion", quote.promotionVersion());
        result.put("lines", quote.lines().stream().map(line -> Map.of("productId", line.product().id(), "name", line.product().name(), "category", line.product().category(), "unitPrice", line.product().unitPrice(), "productDiscountPercent", line.product().productDiscountPercent(), "quantity", line.quantity())).toList());
        result.put("discounts", quote.discounts().stream().map(discount -> Map.of("code", discount.code(), "name", discount.name(), "type", discount.type().name(), "amount", discount.amount(), "calculationBase", discount.calculationBase(), "priority", discount.priority(), "stackable", discount.stackable())).toList());
        return result;
    }

    private Map<String, Object> orderMap(CheckoutOrder order) {
        return Map.of("quoteId", order.quote().id(), "quote", quoteMap(order.quote()), "status", order.status().name(), "confirmedAt", order.confirmedAt().toString());
    }

    @SuppressWarnings("unchecked")
    private Quote toQuote(Map<String, Object> data, String quoteId) {
        List<CartLine> lines = ((List<Map<String, Object>>) data.get("lines")).stream().map(line -> new CartLine(new Product(
                String.valueOf(line.get("productId")), String.valueOf(line.get("name")), String.valueOf(line.get("category")),
                decimal(line.get("unitPrice")), decimal(line.get("productDiscountPercent"))), ((Number) line.get("quantity")).intValue())).toList();
        List<DiscountLine> discounts = ((List<Map<String, Object>>) data.get("discounts")).stream().map(discount -> new DiscountLine(
                String.valueOf(discount.get("code")), String.valueOf(discount.get("name")), DiscountType.valueOf(String.valueOf(discount.get("type"))),
                decimal(discount.get("amount")), decimal(discount.get("calculationBase")), ((Number) discount.get("priority")).intValue(), (Boolean) discount.get("stackable"))).toList();
        return new Quote(quoteId, lines, PaymentMethod.valueOf(String.valueOf(data.get("paymentMethod"))), decimal(data.get("subtotal")), discounts,
                decimal(data.get("shippingCost")), decimal(data.get("total")), Instant.parse(String.valueOf(data.get("expiresAt"))), String.valueOf(data.get("promotionVersion")));
    }

    @SuppressWarnings("unchecked") private Map<String, Object> map(Object value) { return (Map<String, Object>) value; }
    private BigDecimal decimal(Object value) { if (value == null) return BigDecimal.ZERO; return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value)); }
}
