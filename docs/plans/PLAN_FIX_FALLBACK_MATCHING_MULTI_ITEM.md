# Plan: Fix Fallback Matching for Multi-Item Orders

## Problem

Order `2511280A8GJH4C` has a matching click in database:
- Click: `item_id = 24829939126`, `shop_id = 1253280646`
- Order row 38: Same `item_id` and `shop_id`

But fallback matching fails because code only uses `firstItem` to match:

```java
FallbackMatchResult fallbackResult = attemptFallbackMatch(
    platform.getId(),
    firstItem.getItemId(),    // Only checks first item!
    firstItem.getShopId(),
    firstItem.getOrderTime()
);
```

If the order has multiple items and the first item (by CSV row order) doesn't match the click, fallback fails even though another item would match.

## Root Cause

In `ImportShopeeOrdersUseCase.processOrderWithItems()` line 573-578:
- Only `firstItem.getItemId()` and `firstItem.getShopId()` are used
- Other items in the order are ignored for fallback matching

## Solution

Loop through ALL items in the order to find a matching click:

1. Try fallback matching with each item's `itemId` + `shopId`
2. Stop on first successful match
3. If no match found after trying all items, proceed as before (skip order)

## Implementation Steps

### Step 1: Update attemptFallbackMatch to accept list of items

Change the fallback matching section to loop through all items:

**Before:**
```java
if (userId == null && request.getAutoMatch()) {
    FallbackMatchResult fallbackResult = attemptFallbackMatch(
        platform.getId(),
        firstItem.getItemId(),
        firstItem.getShopId(),
        firstItem.getOrderTime()
    );
    // ... handle result
}
```

**After:**
```java
if (userId == null && request.getAutoMatch()) {
    log.info("No tracking code for order {}, attempting fallback matching with {} items...",
        orderId, items.size());

    FallbackMatchResult fallbackResult = FallbackMatchResult.noMatch();

    // Try fallback matching with each item in the order
    for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
        if (item.getItemId() == null || item.getShopId() == null) {
            continue; // Skip items without itemId/shopId
        }

        fallbackResult = attemptFallbackMatch(
            platform.getId(),
            item.getItemId(),
            item.getShopId(),
            item.getOrderTime() != null ? item.getOrderTime() : firstItem.getOrderTime()
        );

        if (fallbackResult.isUniqueMatch() || fallbackResult.hasMultipleMatches()) {
            log.debug("Found fallback match using item {} from shop {}",
                item.getItemId(), item.getShopId());
            break; // Found a match, stop searching
        }
    }

    // ... handle result (same as before)
}
```

## Testing

After implementation:
1. Import the CSV file again
2. Order `2511280A8GJH4C` should now be matched with the click
3. Check that `affiliate_order` table has the order with correct `user_id`

## Files to Modify

1. `cashbee-application/src/main/java/com/cashbee/application/usecase/affiliate/ImportShopeeOrdersUseCase.java`
   - Update fallback matching section (lines 568-607)

## Risk Assessment

- **Low risk**: Only changes fallback matching logic
- **Backward compatible**: Orders with tracking code are not affected
- **Better matching**: Will find matches that were previously missed
