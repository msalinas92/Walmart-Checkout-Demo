package cl.retail.checkout.application;

public class InvalidCheckoutException extends RuntimeException {
    public InvalidCheckoutException(String message) { super(message); }
}
