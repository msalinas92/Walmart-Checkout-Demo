package cl.retail.checkout.application;

import cl.retail.checkout.domain.CheckoutEvent;

public interface CheckoutEventPublisher {
    void publish(CheckoutEvent event);
}
