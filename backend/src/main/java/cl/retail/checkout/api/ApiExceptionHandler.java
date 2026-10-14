package cl.retail.checkout.api;

import cl.retail.checkout.application.InvalidCheckoutException;
import cl.retail.checkout.application.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    record ErrorResponse(String code, String message) { }
    @ExceptionHandler(NotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse notFound(NotFoundException exception) { return new ErrorResponse("NOT_FOUND", exception.getMessage()); }
    @ExceptionHandler(InvalidCheckoutException.class) @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse invalid(InvalidCheckoutException exception) { return new ErrorResponse("INVALID_CHECKOUT", exception.getMessage()); }
}
