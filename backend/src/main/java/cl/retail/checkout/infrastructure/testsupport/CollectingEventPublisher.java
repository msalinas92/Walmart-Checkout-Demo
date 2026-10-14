package cl.retail.checkout.infrastructure.testsupport;

import cl.retail.checkout.application.CheckoutEventPublisher;
import cl.retail.checkout.domain.CheckoutEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CollectingEventPublisher implements CheckoutEventPublisher {
    private final List<CheckoutEvent> events = new CopyOnWriteArrayList<>();
    @Override public void publish(CheckoutEvent event) { events.add(event); }
    public List<CheckoutEvent> events() { return List.copyOf(events); }
}
