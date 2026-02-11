-- ================================================================
-- Migration: Add model_id column to affiliate_order_item
-- Date: 2025-11-26
-- Purpose: Support multiple variants (color/size) of same item in one order
-- ================================================================

-- Step 1: Add model_id column
ALTER TABLE affiliate_order_item
ADD COLUMN model_id VARCHAR(50) NULL AFTER item_id;

-- Step 2: Drop old unique constraint if exists (may have different name)
-- Check existing constraint name first
-- SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
-- WHERE TABLE_NAME = 'affiliate_order_item' AND CONSTRAINT_TYPE = 'UNIQUE';

-- Drop old unique constraint on (order_id, item_id) if exists
ALTER TABLE affiliate_order_item
DROP INDEX IF EXISTS uk_order_item;

-- Step 3: Create new unique constraint on (order_id, item_id, model_id)
-- Note: model_id can be NULL for old data, which is fine in MySQL unique constraints
ALTER TABLE affiliate_order_item
ADD CONSTRAINT uk_order_item_model UNIQUE (order_id, item_id, model_id);

-- Step 4: Add index for model_id lookups
CREATE INDEX idx_item_model ON affiliate_order_item(model_id);

-- Verify changes
DESCRIBE affiliate_order_item;
SHOW INDEX FROM affiliate_order_item;
