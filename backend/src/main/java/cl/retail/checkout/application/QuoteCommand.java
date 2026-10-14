package cl.retail.checkout.application;

import cl.retail.checkout.domain.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuoteCommand(@NotEmpty List<Item> items, @NotNull PaymentMethod paymentMethod) {
    public record Item(@NotBlank String productId, @Min(1) int quantity) { }
}
