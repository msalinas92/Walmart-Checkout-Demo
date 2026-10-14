package cl.retail.checkout.api;

import cl.retail.checkout.application.CheckoutRepository;
import cl.retail.checkout.domain.PaymentMethod;
import cl.retail.checkout.domain.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catálogo", description = "Datos ficticios para la demo")
public class CatalogController {
    private final CheckoutRepository repository;
    public CatalogController(CheckoutRepository repository) { this.repository = repository; }
    @GetMapping("/catalog/products") @Operation(summary = "Busca productos almacenados en Firestore")
    List<Product> products(@RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "") String category) {
        return repository.searchProducts(query, category);
    }
    @GetMapping("/catalog/categories") @Operation(summary = "Lista categorías disponibles en Firestore")
    List<String> categories() {
        return repository.searchProducts("", "").stream().map(Product::category).distinct()
                .sorted(Comparator.naturalOrder()).toList();
    }
    @GetMapping("/payment-methods") @Operation(summary = "Lista medios de pago")
    List<PaymentMethod> paymentMethods() { return List.of(PaymentMethod.values()); }
}
