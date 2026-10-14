package cl.retail.checkout.application;

import cl.retail.checkout.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CheckoutService {
    private final CheckoutRepository repository;
    private final CheckoutEventPublisher eventPublisher;

    public CheckoutService(CheckoutRepository repository, CheckoutEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public Quote quote(QuoteCommand command) {
        Map<String, Product> products = repository.findProducts(command.items().stream().map(QuoteCommand.Item::productId).toList())
                .stream().collect(Collectors.toMap(Product::id, Function.identity()));
        List<CartLine> lines = command.items().stream().map(item -> {
            Product product = products.get(item.productId());
            if (product == null) throw new NotFoundException("No existe el producto " + item.productId());
            return new CartLine(product, item.quantity());
        }).toList();
        // Aquí construyo el motor desde las promociones activas para que Firestore gobierne las reglas sin redesplegar código.
        List<PromotionRule> configuredRules = repository.findActivePromotions().stream()
                .map(ConfiguredPromotionRule::new).map(PromotionRule.class::cast).toList();
        Quote quote = new PricingEngine(java.util.stream.Stream.concat(
                java.util.stream.Stream.of(new ProductDiscountRule()), configuredRules.stream()).toList())
                .quote(lines, command.paymentMethod());
        repository.saveQuote(quote);
        return quote;
    }

    public CheckoutOrder confirm(ConfirmCommand command, String idempotencyKey) {
        CheckoutOrder order = repository.findOrderByIdempotencyKey(idempotencyKey).orElseGet(() -> createOrder(command, idempotencyKey));
        if (repository.isEventPending(order.id())) {
            // Aquí reintento de forma segura: el eventId estable permite deduplicar si Pub/Sub recibió el intento anterior.
            eventPublisher.publish(toEvent(order));
            repository.markEventPublished(order.id());
        }
        return order;
    }

    private CheckoutOrder createOrder(ConfirmCommand command, String idempotencyKey) {
        Quote quote = repository.findQuote(command.quoteId())
                .orElseThrow(() -> new NotFoundException("No existe la cotización " + command.quoteId()));
        if (quote.expiresAt().isBefore(Instant.now())) throw new InvalidCheckoutException("La cotización expiró; vuelve a cotizar.");
        CheckoutOrder order = new CheckoutOrder("ord_" + UUID.randomUUID(), quote, OrderStatus.PAYMENT_SIMULATED, Instant.now());
        // Aquí persisto orden, idempotencia y outbox antes de publicar para no anunciar compras inexistentes.
        repository.saveOrder(order, idempotencyKey);
        return order;
    }

    public CheckoutOrder findOrder(String orderId) {
        return repository.findOrder(orderId).orElseThrow(() -> new NotFoundException("No existe la orden " + orderId));
    }

    private CheckoutEvent toEvent(CheckoutOrder order) {
        BigDecimal product = discount(order.quote(), DiscountType.PRODUCT);
        BigDecimal cart = discount(order.quote(), DiscountType.CART);
        BigDecimal payment = discount(order.quote(), DiscountType.PAYMENT_METHOD);
        return new CheckoutEvent("evt_" + order.id().substring("ord_".length()), "checkout.transaction.v1", order.confirmedAt(), order.id(),
                order.quote().subtotal(), product, cart, payment, order.quote().total(),
                order.quote().paymentMethod().name(), order.quote().discounts().stream().map(DiscountLine::code).toList(), 1);
    }

    private BigDecimal discount(Quote quote, DiscountType type) {
        return quote.discounts().stream().filter(discount -> discount.type() == type)
                .map(DiscountLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
