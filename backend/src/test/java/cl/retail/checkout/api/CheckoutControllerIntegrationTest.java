package cl.retail.checkout.api;

import cl.retail.checkout.TestCheckoutConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestCheckoutConfiguration.class)
class CheckoutControllerIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void returnsAnExplainableQuote() throws Exception {
        mockMvc.perform(post("/api/v1/checkouts/quote").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"tv-001\",\"quantity\":1}],\"paymentMethod\":\"DEBIT\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(299990))
                .andExpect(jsonPath("$.discounts[0].code").value("PRODUCT_OFFER"))
                .andExpect(jsonPath("$.discounts[2].code").value("DEBIT_10"));
    }

    @Test
    void searchesTheFirestoreBackedCatalogByCategoryAndText() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products")
                        .queryParam("query", "sábanas")
                        .queryParam("category", "Hogar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("home-001"))
                .andExpect(jsonPath("$[0].category").value("Hogar"));
    }

    @Test
    void listsDistinctCatalogCategories() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Despensa"))
                .andExpect(jsonPath("$[1]").value("Hogar"))
                .andExpect(jsonPath("$[2]").value("Tecnología"));
    }

    @Test
    void exposesTheOpenApiContractUsedBySwaggerUi() throws Exception {
        // Aquí verifico el contrato generado para detectar incompatibilidades al actualizar springdoc.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Retail Checkout API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/checkouts/quote']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/checkouts/confirm']").exists());
    }
}
