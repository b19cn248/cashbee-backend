package com.cashbee.application.util.affiliate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to parse Shopee affiliate commission CSV files.
 *
 * CSV format from Shopee:
 * - UTF-8 with BOM
 * - Comma-separated with quoted fields
 * - Vietnamese column names
 * - Date format: yyyy-MM-dd HH:mm:ss
 *
 * This parser uses column names (not indices) for robustness against CSV format changes.
 *
 * @author CashBee Team
 */
@Component
@Slf4j
public class ShopeeCSVParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    // CSV column names from Shopee (Vietnamese)
    // Using column names instead of indices for robustness
    private static final String COL_ORDER_ID = "ID đơn hàng";
    private static final String COL_ORDER_STATUS = "Trạng thái đặt hàng";
    private static final String COL_CHECKOUT_ID = "Checkout id";
    private static final String COL_ORDER_TIME = "Thời Gian Đặt Hàng";
    private static final String COL_COMPLETE_TIME = "Thời gian hoàn thành";
    private static final String COL_CLICK_TIME = "Thời gian Click";
    private static final String COL_SHOP_NAME = "Tên Shop";
    private static final String COL_SHOP_ID = "Shop id";
    private static final String COL_SHOP_TYPE = "Loại Shop";
    private static final String COL_ITEM_ID = "Item id";
    private static final String COL_ITEM_NAME = "Tên Item";
    private static final String COL_MODEL_ID = "ID Model";
    private static final String COL_PRODUCT_TYPE = "Loại sản phẩm";
    private static final String COL_CATEGORY_LV1 = "L1 Danh mục toàn cầu";
    private static final String COL_CATEGORY_LV2 = "L2 Danh mục toàn cầu";
    private static final String COL_CATEGORY_LV3 = "L3 Danh mục toàn cầu";
    private static final String COL_PRICE = "Giá(₫)";
    private static final String COL_QUANTITY = "Số lượng";
    private static final String COL_COMMISSION_TYPE = "Loại Hoa hồng";
    private static final String COL_ORDER_VALUE = "Giá trị đơn hàng (₫)";
    private static final String COL_REFUND_AMOUNT = "Số tiền hoàn trả (₫)";
    private static final String COL_SHOPEE_COMMISSION_RATE = "Tỷ lệ sản phẩm hoa hồng Shope";
    private static final String COL_SHOPEE_PRODUCT_COMMISSION = "Hoa hồng Shopee trên sản phẩm(₫)";
    private static final String COL_SELLER_COMMISSION_RATE = "Tỷ lệ sản phẩm hoa hồng người bán";
    private static final String COL_XTRA_PRODUCT_COMMISSION = "Hoa hồng Xtra trên sản phẩm(₫)";
    private static final String COL_TOTAL_PRODUCT_COMMISSION = "Tổng hoa hồng sản phẩm(₫)";
    private static final String COL_SHOPEE_ORDER_COMMISSION = "Hoa hồng đơn hàng từ Shopee(₫)";
    private static final String COL_SELLER_ORDER_COMMISSION = "Hoa hồng đơn hàng từ Người bán(₫)";
    private static final String COL_TOTAL_ORDER_COMMISSION = "Tổng hoa hồng đơn hàng(₫)";
    private static final String COL_NET_AFFILIATE_COMMISSION = "Hoa hồng ròng tiếp thị liên kết(₫)";
    private static final String COL_PRODUCT_STATUS = "Trạng thái sản phẩm liên kết";
    private static final String COL_PRODUCT_NOTE = "Ghi chú sản phẩm";
    private static final String COL_SUB_ID1 = "Sub_id1";
    private static final String COL_SUB_ID2 = "Sub_id2";
    private static final String COL_SUB_ID3 = "Sub_id3";

    /**
     * Parse Shopee CSV file and extract order data.
     *
     * @param inputStream CSV file input stream
     * @return List of parsed order records
     * @throws IOException if file reading fails
     */
    public List<ShopeeOrderRecord> parse(InputStream inputStream) throws IOException {
        List<ShopeeOrderRecord> records = new ArrayList<>();

        // Use Apache Commons CSV to handle quoted fields properly
        // Wrap InputStreamReader in BufferedReader to support mark()
        try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // Skip BOM if present
            bufferedReader.mark(1);
            int firstChar = bufferedReader.read();
            if (firstChar != 0xFEFF) {
                bufferedReader.reset();  // No BOM, go back
            }

            // Configure CSV format
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()  // First line is header
                .setSkipHeaderRecord(true)  // Skip header when parsing
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

            CSVParser csvParser = csvFormat.parse(bufferedReader);

            log.info("CSV headers: {}", csvParser.getHeaderNames());

            // Validate that required columns exist
            validateHeaders(csvParser);

            int rowNumber = 1;  // Row 1 is first data row (after header)

            for (CSVRecord csvRecord : csvParser) {
                rowNumber++;

                try {
                    ShopeeOrderRecord record = parseLine(csvRecord, rowNumber);
                    records.add(record);
                } catch (Exception e) {
                    log.error("Failed to parse CSV row {}: {}", rowNumber, e.getMessage(), e);
                    // Create error record
                    ShopeeOrderRecord errorRecord = ShopeeOrderRecord.builder()
                        .rowNumber(rowNumber)
                        .rawData(csvRecord.toString())
                        .parseError(e.getMessage())
                        .build();
                    records.add(errorRecord);
                }
            }
        }

        log.info("Parsed {} records from CSV", records.size());
        return records;
    }

    /**
     * Validate that required CSV columns exist.
     *
     * @param csvParser CSV parser with headers loaded
     */
    private void validateHeaders(CSVParser csvParser) {
        List<String> headers = csvParser.getHeaderNames();
        List<String> requiredColumns = List.of(
            COL_ORDER_ID,
            COL_ORDER_STATUS,
            COL_SUB_ID1,
            COL_TOTAL_ORDER_COMMISSION,
            COL_TOTAL_PRODUCT_COMMISSION
        );

        for (String column : requiredColumns) {
            if (!headers.contains(column)) {
                log.warn("Required column '{}' not found in CSV. Available columns: {}",
                    column, headers);
            }
        }

        log.debug("CSV has {} columns. Required columns validated.", headers.size());
    }

    /**
     * Parse a single CSV record using column names.
     *
     * @param csvRecord CSV record from Apache Commons CSV
     * @param rowNumber Row number (for error reporting)
     * @return Parsed order record
     */
    private ShopeeOrderRecord parseLine(CSVRecord csvRecord, int rowNumber) {
        return ShopeeOrderRecord.builder()
            .rowNumber(rowNumber)
            .rawData(csvRecord.toString())
            .orderId(getColumnByName(csvRecord, COL_ORDER_ID))
            .orderStatus(getColumnByName(csvRecord, COL_ORDER_STATUS))
            .checkoutId(getColumnByName(csvRecord, COL_CHECKOUT_ID))
            .orderTime(parseDateTime(getColumnByName(csvRecord, COL_ORDER_TIME)))
            .completeTime(parseDateTime(getColumnByName(csvRecord, COL_COMPLETE_TIME)))
            .clickTime(parseDateTime(getColumnByName(csvRecord, COL_CLICK_TIME)))
            .shopName(getColumnByName(csvRecord, COL_SHOP_NAME))
            .shopId(getColumnByName(csvRecord, COL_SHOP_ID))
            .shopType(getColumnByName(csvRecord, COL_SHOP_TYPE))
            .itemId(getColumnByName(csvRecord, COL_ITEM_ID))
            .itemName(getColumnByName(csvRecord, COL_ITEM_NAME))
            .modelId(getColumnByName(csvRecord, COL_MODEL_ID))
            .productType(getColumnByName(csvRecord, COL_PRODUCT_TYPE))
            .categoryLv1(getColumnByName(csvRecord, COL_CATEGORY_LV1))
            .categoryLv2(getColumnByName(csvRecord, COL_CATEGORY_LV2))
            .categoryLv3(getColumnByName(csvRecord, COL_CATEGORY_LV3))
            .price(parseBigDecimal(getColumnByName(csvRecord, COL_PRICE)))
            .quantity(parseInteger(getColumnByName(csvRecord, COL_QUANTITY)))
            .commissionType(getColumnByName(csvRecord, COL_COMMISSION_TYPE))
            .orderValue(parseBigDecimal(getColumnByName(csvRecord, COL_ORDER_VALUE)))
            .refundAmount(parseBigDecimal(getColumnByName(csvRecord, COL_REFUND_AMOUNT)))
            .shopeeCommissionRate(parseBigDecimal(getColumnByName(csvRecord, COL_SHOPEE_COMMISSION_RATE)))
            .shopeeProductCommission(parseBigDecimal(getColumnByName(csvRecord, COL_SHOPEE_PRODUCT_COMMISSION)))
            .sellerCommissionRate(parseBigDecimal(getColumnByName(csvRecord, COL_SELLER_COMMISSION_RATE)))
            .xtraProductCommission(parseBigDecimal(getColumnByName(csvRecord, COL_XTRA_PRODUCT_COMMISSION)))
            .totalProductCommission(parseBigDecimal(getColumnByName(csvRecord, COL_TOTAL_PRODUCT_COMMISSION)))
            .shopeeOrderCommission(parseBigDecimal(getColumnByName(csvRecord, COL_SHOPEE_ORDER_COMMISSION)))
            .sellerOrderCommission(parseBigDecimal(getColumnByName(csvRecord, COL_SELLER_ORDER_COMMISSION)))
            .totalOrderCommission(parseBigDecimal(getColumnByName(csvRecord, COL_TOTAL_ORDER_COMMISSION)))
            .netAffiliateCommission(parseBigDecimal(getColumnByName(csvRecord, COL_NET_AFFILIATE_COMMISSION)))
            .productStatus(getColumnByName(csvRecord, COL_PRODUCT_STATUS))
            .productNote(getColumnByName(csvRecord, COL_PRODUCT_NOTE))
            .subId1(getColumnByName(csvRecord, COL_SUB_ID1))
            .subId2(getColumnByName(csvRecord, COL_SUB_ID2))
            .subId3(getColumnByName(csvRecord, COL_SUB_ID3))
            .build();
    }

    /**
     * Get column value by name from CSVRecord.
     * This method is robust against column order changes.
     *
     * @param csvRecord CSV record
     * @param columnName Column name to retrieve
     * @return Column value, or null if not found or empty
     */
    private String getColumnByName(CSVRecord csvRecord, String columnName) {
        try {
            // Check if column exists in the record
            if (csvRecord.isMapped(columnName)) {
                String value = csvRecord.get(columnName);
                if (value != null) {
                    value = value.trim();
                    return value.isEmpty() ? null : value;
                }
            } else {
                log.debug("Column '{}' not mapped in CSV record {}", columnName, csvRecord.getRecordNumber());
            }
        } catch (IllegalArgumentException e) {
            log.debug("Column '{}' not found in CSV: {}", columnName, e.getMessage());
        } catch (Exception e) {
            log.debug("Failed to get column '{}' from record: {}", columnName, e.getMessage());
        }
        return null;
    }

    /**
     * Parse date time string.
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse datetime: {}", value);
            return null;
        }
    }

    /**
     * Parse BigDecimal.
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            // Remove currency symbols, commas, and percentage signs
            String cleaned = value.replace("₫", "")
                .replace(",", "")
                .replace("%", "")
                .replace(" ", "")
                .trim();

            if (cleaned.isEmpty()) {
                return null;
            }

            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.debug("Failed to parse BigDecimal: {}", value);
            return null;
        }
    }

    /**
     * Parse integer.
     */
    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            String cleaned = value.replace(",", "").trim();
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            log.debug("Failed to parse Integer: {}", value);
            return null;
        }
    }

    /**
     * DTO for parsed Shopee order record.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopeeOrderRecord {
        /**
         * Row number in CSV (for error reporting).
         */
        private Integer rowNumber;

        /**
         * Raw CSV line data.
         */
        private String rawData;

        /**
         * Parse error message (if parsing failed).
         */
        private String parseError;

        /**
         * Order ID from Shopee.
         */
        private String orderId;

        /**
         * Order status (e.g., "Đang chờ xử lý", "Hoàn thành").
         */
        private String orderStatus;

        /**
         * Checkout ID (groups multiple items in same checkout).
         */
        private String checkoutId;

        /**
         * Order timestamp.
         */
        private LocalDateTime orderTime;

        /**
         * Complete time (when order was completed).
         */
        private LocalDateTime completeTime;

        /**
         * Click time (when user clicked affiliate link).
         */
        private LocalDateTime clickTime;

        /**
         * Shop name.
         */
        private String shopName;

        /**
         * Shop ID.
         */
        private String shopId;

        /**
         * Shop type (e.g., "Preferred(Non-CB)", "Shopee Mall(Non-CB)").
         */
        private String shopType;

        /**
         * Item ID (Product ID).
         */
        private String itemId;

        /**
         * Item name (Product name).
         */
        private String itemName;

        /**
         * Model ID (Product variant ID).
         */
        private String modelId;

        /**
         * Product type (e.g., "Normal Product").
         */
        private String productType;

        /**
         * Category level 1.
         */
        private String categoryLv1;

        /**
         * Category level 2.
         */
        private String categoryLv2;

        /**
         * Category level 3.
         */
        private String categoryLv3;

        /**
         * Product price.
         */
        private BigDecimal price;

        /**
         * Quantity ordered.
         */
        private Integer quantity;

        /**
         * Commission type (e.g., "Shopee Comm", "XTRA Comm").
         */
        private String commissionType;

        /**
         * Order value (actual order amount after discount).
         */
        private BigDecimal orderValue;

        /**
         * Refund amount (if any).
         */
        private BigDecimal refundAmount;

        /**
         * Shopee commission rate (%).
         */
        private BigDecimal shopeeCommissionRate;

        /**
         * Shopee commission on product (VND).
         */
        private BigDecimal shopeeProductCommission;

        /**
         * Seller commission rate (%).
         */
        private BigDecimal sellerCommissionRate;

        /**
         * Xtra commission on product (VND).
         */
        private BigDecimal xtraProductCommission;

        /**
         * Total product commission (VND).
         */
        private BigDecimal totalProductCommission;

        /**
         * Shopee order commission (VND).
         */
        private BigDecimal shopeeOrderCommission;

        /**
         * Seller order commission (VND).
         */
        private BigDecimal sellerOrderCommission;

        /**
         * Total order commission (VND) - IMPORTANT for cashback calculation.
         */
        private BigDecimal totalOrderCommission;

        /**
         * Net affiliate commission after fees (VND).
         */
        private BigDecimal netAffiliateCommission;

        /**
         * Product status (e.g., "Đang chờ xử lý", "Hoàn thành").
         */
        private String productStatus;

        /**
         * Product note (reason for status).
         */
        private String productNote;

        /**
         * Custom parameter 1 (contains tracking code).
         */
        private String subId1;

        /**
         * Custom parameter 2.
         */
        private String subId2;

        /**
         * Custom parameter 3.
         */
        private String subId3;

        /**
         * Check if this record has a parse error.
         */
        public boolean hasError() {
            return parseError != null && !parseError.isEmpty();
        }

        /**
         * Check if this record has tracking code in Sub_id1.
         */
        public boolean hasTrackingCode() {
            return subId1 != null && !subId1.isEmpty();
        }

        /**
         * Get tracking code from Sub_id1.
         */
        public String getTrackingCode() {
            return subId1;
        }

        /**
         * Check if order is completed (eligible for commission).
         */
        public boolean isCompleted() {
            // "Hoàn thành" means completed in Vietnamese
            return orderStatus != null &&
                (orderStatus.contains("Hoàn thành") || orderStatus.equalsIgnoreCase("Completed"));
        }

        /**
         * Check if order is cancelled.
         */
        public boolean isCancelled() {
            // "Đã hủy" means cancelled in Vietnamese
            return orderStatus != null &&
                (orderStatus.contains("Đã hủy") || orderStatus.equalsIgnoreCase("Cancelled"));
        }

        /**
         * Get the commission amount to use for cashback calculation.
         * Priority: totalOrderCommission > totalProductCommission > 0
         */
        public BigDecimal getCommissionForCashback() {
            if (totalOrderCommission != null && totalOrderCommission.compareTo(BigDecimal.ZERO) > 0) {
                return totalOrderCommission;
            }
            if (totalProductCommission != null && totalProductCommission.compareTo(BigDecimal.ZERO) > 0) {
                return totalProductCommission;
            }
            return BigDecimal.ZERO;
        }
    }
}
