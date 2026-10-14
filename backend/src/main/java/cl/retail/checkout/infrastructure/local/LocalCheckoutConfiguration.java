package cl.retail.checkout.infrastructure.local;

import cl.retail.checkout.application.CheckoutEventPublisher;
import cl.retail.checkout.application.CheckoutRepository;
import cl.retail.checkout.application.CheckoutService;
import cl.retail.checkout.domain.Product;
import cl.retail.checkout.domain.PaymentMethod;
import cl.retail.checkout.domain.PromotionDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Configuración predeterminada para desarrollar y evaluar la aplicación sin GCP. */
@Configuration
@Profile("!gcp & !test")
@EnableTransactionManagement
public class LocalCheckoutConfiguration {
    @Bean
    DataSource localDataSource(@Value("${checkout.local.database-path}") String databasePath) {
        try {
            Path path = Path.of(databasePath).toAbsolutePath().normalize();
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:" + path);
            return dataSource;
        } catch (Exception exception) {
            throw new IllegalStateException("No pude preparar la base SQLite local", exception);
        }
    }

    @Bean JdbcTemplate jdbcTemplate(DataSource localDataSource) { return new JdbcTemplate(localDataSource); }
    @Bean PlatformTransactionManager transactionManager(DataSource localDataSource) { return new DataSourceTransactionManager(localDataSource); }

    @Bean
    CheckoutRepository checkoutRepository(JdbcTemplate jdbcTemplate, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        SqliteCheckoutRepository repository = new SqliteCheckoutRepository(jdbcTemplate, objectMapper);
        repository.initialize();
        repository.seedProducts(List.of(
                new Product("tv-001", "Smart TV 55 pulgadas", "Tecnología", BigDecimal.valueOf(299_990), BigDecimal.valueOf(15)),
                new Product("food-001", "Pack café premium", "Despensa", BigDecimal.valueOf(24_990), BigDecimal.ZERO),
                new Product("home-001", "Set de sábanas king", "Hogar", BigDecimal.valueOf(89_990), BigDecimal.valueOf(10)),
                new Product("laptop-001", "Notebook 14 pulgadas", "Tecnología", BigDecimal.valueOf(649_990), BigDecimal.valueOf(8)),
                new Product("phone-001", "Smartphone 5G 256 GB", "Tecnología", BigDecimal.valueOf(399_990), BigDecimal.valueOf(12)),
                new Product("audio-001", "Audífonos inalámbricos", "Tecnología", BigDecimal.valueOf(59_990), BigDecimal.valueOf(20)),
                new Product("home-002", "Juego de toallas premium", "Hogar", BigDecimal.valueOf(34_990), BigDecimal.ZERO),
                new Product("food-002", "Canasta desayuno familiar", "Despensa", BigDecimal.valueOf(45_990), BigDecimal.valueOf(5)),
                new Product("appliance-001", "Freidora de aire 5 litros", "Electrodomésticos", BigDecimal.valueOf(79_990), BigDecimal.valueOf(18)),
                new Product("appliance-002", "Cafetera programable", "Electrodomésticos", BigDecimal.valueOf(49_990), BigDecimal.valueOf(10)),
                new Product("sport-001", "Bicicleta urbana aro 29", "Deportes", BigDecimal.valueOf(229_990), BigDecimal.valueOf(15)),
                new Product("sport-002", "Set de mancuernas 20 kg", "Deportes", BigDecimal.valueOf(69_990), BigDecimal.ZERO)));
        repository.seedPromotions(List.of(
                new PromotionDefinition("CART_5_OVER_200K", "5% por compras sobre $200.000", "CART_THRESHOLD", BigDecimal.valueOf(5), BigDecimal.valueOf(200_000), BigDecimal.ZERO, null, 200, true),
                new PromotionDefinition("DEBIT_10", "10% de descuento pagando con débito", "PAYMENT_METHOD", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.valueOf(50_000), PaymentMethod.DEBIT, 300, true)));
        return repository;
    }

    @Bean
    CheckoutEventPublisher eventPublisher() {
        return new LoggingCheckoutEventPublisher();
    }

    @Bean
    CheckoutService checkoutService(CheckoutRepository repository, CheckoutEventPublisher publisher) {
        return new CheckoutService(repository, publisher);
    }
}
