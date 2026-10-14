package cl.retail.checkout.application;

import jakarta.validation.constraints.NotBlank;

public record ConfirmCommand(@NotBlank String quoteId) { }
