package cl.retail.checkout.api;

import cl.retail.checkout.application.CheckoutService;
import cl.retail.checkout.application.ConfirmCommand;
import cl.retail.checkout.application.QuoteCommand;
import cl.retail.checkout.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Checkout", description = "Cotización y confirmación de compras retail")
public class CheckoutController {
    private final CheckoutService service;
    public CheckoutController(CheckoutService service) { this.service = service; }

    @PostMapping("/checkouts/quote")
    @Operation(summary = "Cotiza un carrito", description = "Calcula precios y descuentos sin crear una orden.")
    QuoteResponse quote(@Valid @RequestBody QuoteCommand command) { return QuoteResponse.from(service.quote(command)); }

    @PostMapping("/checkouts/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Confirma una compra", description = "Simula el pago y publica el evento de negocio.")
    OrderResponse confirm(@Valid @RequestBody ConfirmCommand command,
                          @Parameter(description = "Clave única para reintentos seguros", required = true)
                          @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return OrderResponse.from(service.confirm(command, idempotencyKey));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Busca una orden confirmada")
    OrderResponse findOrder(@PathVariable String orderId) { return OrderResponse.from(service.findOrder(orderId)); }

    public record QuoteResponse(String quoteId, PaymentMethod paymentMethod, BigDecimal subtotal, List<DiscountLine> discounts,
                                BigDecimal shippingCost, BigDecimal total, Instant expiresAt, String promotionVersion) {
        static QuoteResponse from(Quote quote) { return new QuoteResponse(quote.id(), quote.paymentMethod(), quote.subtotal(), quote.discounts(), quote.shippingCost(), quote.total(), quote.expiresAt(), quote.promotionVersion()); }
    }
    public record OrderResponse(String orderId, OrderStatus status, Instant confirmedAt, QuoteResponse quote) {
        static OrderResponse from(CheckoutOrder order) { return new OrderResponse(order.id(), order.status(), order.confirmedAt(), QuoteResponse.from(order.quote())); }
    }
}
