package cl.retail.checkout.infrastructure.local;

import cl.retail.checkout.application.CheckoutEventPublisher;
import cl.retail.checkout.domain.CheckoutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simula la publicación del evento de negocio durante la ejecución local. */
public final class LoggingCheckoutEventPublisher implements CheckoutEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(LoggingCheckoutEventPublisher.class);

    @Override
    public void publish(CheckoutEvent event) {
        log.info("Evento local simulado: type={}, orderId={}, eventId={}",
                event.eventType(), event.orderId(), event.eventId());
    }
}
