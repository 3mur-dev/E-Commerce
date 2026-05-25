ALTER TABLE "orders" DROP CONSTRAINT IF EXISTS "orders_status_check";

UPDATE "orders"
SET "status" = 'PROCESSING'
WHERE "status" = 'PAID';

ALTER TABLE "orders"
ADD CONSTRAINT "orders_status_check"
CHECK ("status" IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'));
