package cl.retail.checkout.infrastructure.local;

import cl.retail.checkout.application.CheckoutRepository;
import cl.retail.checkout.domain.CheckoutOrder;
import cl.retail.checkout.domain.PaymentMethod;
import cl.retail.checkout.domain.Product;
import cl.retail.checkout.domain.PromotionDefinition;
import cl.retail.checkout.domain.Quote;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Adaptador SQLite persistente para ejecutar el checkout localmente sin GCP. */
public class SqliteCheckoutRepository implements CheckoutRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SqliteCheckoutRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void initialize() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS products (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, unit_price NUMERIC NOT NULL, product_discount_percent NUMERIC NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS promotions (code TEXT PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, percent NUMERIC NOT NULL, minimum_amount NUMERIC NOT NULL, max_discount NUMERIC, payment_method TEXT, priority INTEGER NOT NULL, active INTEGER NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS quotes (id TEXT PRIMARY KEY, payload TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS orders (id TEXT PRIMARY KEY, payload TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS idempotency_keys (idempotency_key TEXT PRIMARY KEY, order_id TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS outbox_events (order_id TEXT PRIMARY KEY, status TEXT NOT NULL)");
    }

    public void seedProducts(List<Product> products) {
        for (Product product : products) {
            jdbc.update("INSERT OR IGNORE INTO products (id, name, category, unit_price, product_discount_percent) VALUES (?, ?, ?, ?, ?)",
                    product.id(), product.name(), product.category(), product.unitPrice(), product.productDiscountPercent());
        }
    }

    public void seedPromotions(List<PromotionDefinition> promotions) {
        for (PromotionDefinition promotion : promotions) {
            jdbc.update("INSERT OR IGNORE INTO promotions (code, name, type, percent, minimum_amount, max_discount, payment_method, priority, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    promotion.code(), promotion.name(), promotion.type(), promotion.percent(), promotion.minimumAmount(), promotion.maxDiscount(),
                    promotion.paymentMethod() == null ? null : promotion.paymentMethod().name(), promotion.priority(), promotion.active() ? 1 : 0);
        }
    }

    @Override
    public List<Product> findProducts(List<String> ids) {
        return ids.stream().distinct().flatMap(id -> jdbc.query("SELECT id, name, category, unit_price, product_discount_percent FROM products WHERE id = ?", this::product, id).stream()).toList();
    }

    @Override
    public List<Product> searchProducts(String query, String category) {
        String term = query == null ? "" : query.trim().toLowerCase();
        String selectedCategory = category == null ? "" : category.trim();
        return jdbc.query("SELECT id, name, category, unit_price, product_discount_percent FROM products WHERE (? = '' OR lower(name) LIKE ?) AND (? = '' OR lower(category) = lower(?)) ORDER BY name",
                this::product, term, "%" + term + "%", selectedCategory, selectedCategory);
    }

    @Override
    public List<PromotionDefinition> findActivePromotions() {
        return jdbc.query("SELECT code, name, type, percent, minimum_amount, max_discount, payment_method, priority, active FROM promotions WHERE active = 1 ORDER BY priority, code",
                (resultSet, row) -> new PromotionDefinition(resultSet.getString("code"), resultSet.getString("name"), resultSet.getString("type"),
                        resultSet.getBigDecimal("percent"), resultSet.getBigDecimal("minimum_amount"), resultSet.getBigDecimal("max_discount"),
                        resultSet.getString("payment_method") == null ? null : PaymentMethod.valueOf(resultSet.getString("payment_method")),
                        resultSet.getInt("priority"), resultSet.getBoolean("active")));
    }

    @Override public void saveQuote(Quote quote) { jdbc.update("INSERT INTO quotes (id, payload) VALUES (?, ?)", quote.id(), json(quote)); }
    @Override public Optional<Quote> findQuote(String quoteId) { return jdbc.query("SELECT payload FROM quotes WHERE id = ?", (resultSet, row) -> quote(resultSet.getString(1)), quoteId).stream().findFirst(); }

    @Override
    @Transactional
    public void saveOrder(CheckoutOrder order, String idempotencyKey) {
        jdbc.update("INSERT INTO orders (id, payload) VALUES (?, ?)", order.id(), json(order));
        jdbc.update("INSERT INTO idempotency_keys (idempotency_key, order_id) VALUES (?, ?)", idempotencyKey, order.id());
        jdbc.update("INSERT INTO outbox_events (order_id, status) VALUES (?, 'PENDING')", order.id());
    }

    @Override public boolean isEventPending(String orderId) { return !jdbc.query("SELECT 1 FROM outbox_events WHERE order_id = ? AND status = 'PENDING'", (resultSet, row) -> resultSet.getInt(1), orderId).isEmpty(); }
    @Override public void markEventPublished(String orderId) { jdbc.update("UPDATE outbox_events SET status = 'PUBLISHED' WHERE order_id = ?", orderId); }
    @Override public Optional<CheckoutOrder> findOrderByIdempotencyKey(String key) { return jdbc.query("SELECT o.payload FROM orders o JOIN idempotency_keys k ON k.order_id = o.id WHERE k.idempotency_key = ?", (resultSet, row) -> order(resultSet.getString(1)), key).stream().findFirst(); }
    @Override public Optional<CheckoutOrder> findOrder(String id) { return jdbc.query("SELECT payload FROM orders WHERE id = ?", (resultSet, row) -> order(resultSet.getString(1)), id).stream().findFirst(); }

    private Product product(java.sql.ResultSet resultSet, int row) throws java.sql.SQLException {
        return new Product(resultSet.getString("id"), resultSet.getString("name"), resultSet.getString("category"),
                resultSet.getBigDecimal("unit_price"), resultSet.getBigDecimal("product_discount_percent"));
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("No pude serializar datos de checkout local", exception); }
    }
    private Quote quote(String value) { return read(value, Quote.class); }
    private CheckoutOrder order(String value) { return read(value, CheckoutOrder.class); }
    private <T> T read(String value, Class<T> type) {
        try { return objectMapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("No pude leer datos de checkout local", exception); }
    }
}
