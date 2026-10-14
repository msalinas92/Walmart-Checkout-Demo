package cl.retail.checkout.domain;

import java.math.BigDecimal;

public record PromotionDefinition(String code, String name, String type, BigDecimal percent,
                                  BigDecimal minimumAmount, BigDecimal maxDiscount,
                                  PaymentMethod paymentMethod, int priority, boolean active) { }
