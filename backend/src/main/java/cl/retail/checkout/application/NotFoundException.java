package cl.retail.checkout.application;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
