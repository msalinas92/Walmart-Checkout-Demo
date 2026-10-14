CREATE SCHEMA IF NOT EXISTS `retail_analytics`;

CREATE TABLE IF NOT EXISTS `retail_analytics.checkout_transactions_raw` (
  eventId STRING NOT NULL,
  eventType STRING NOT NULL,
  occurredAt TIMESTAMP NOT NULL,
  orderId STRING NOT NULL,
  subtotal NUMERIC NOT NULL,
  productDiscount NUMERIC NOT NULL,
  cartDiscount NUMERIC NOT NULL,
  paymentDiscount NUMERIC NOT NULL,
  finalTotal NUMERIC NOT NULL,
  paymentMethod STRING NOT NULL,
  promotionCodes ARRAY<STRING>,
  schemaVersion INT64 NOT NULL
)
PARTITION BY DATE(occurredAt)
CLUSTER BY paymentMethod, orderId;

CREATE OR REPLACE VIEW `retail_analytics.checkout_transactions` AS
SELECT * EXCEPT(row_number)
FROM (
  SELECT *, ROW_NUMBER() OVER (PARTITION BY eventId ORDER BY occurredAt DESC) AS row_number
  FROM `retail_analytics.checkout_transactions_raw`
)
WHERE row_number = 1;
