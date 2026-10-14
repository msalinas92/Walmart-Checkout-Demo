package cl.retail.checkout.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI retailCheckoutOpenApi() {
        return new OpenAPI().info(new Info().title("Retail Checkout API").version("v1")
                .description("API para cotizar y confirmar compras con promociones explicables."));
    }
}
