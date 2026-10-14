package cl.retail.checkout.domain;

import java.math.BigDecimal;

public record Product(String id, String name, String category, BigDecimal unitPrice, BigDecimal productDiscountPercent) { }
