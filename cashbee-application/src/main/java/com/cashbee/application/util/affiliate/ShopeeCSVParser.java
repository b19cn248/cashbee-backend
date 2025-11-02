package com.cashbee.application.util.affiliate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * - Comma-separated
 * - Vietnamese column names
 * - Date format: yyyy-MM-dd HH:mm:ss
 *
 * Key columns:
 * - ID đơn hàng (Order ID)
 * - Trạng thái đặt hàng (Order Status)
 * - Shop id
 * - Item id
 * - Sub_id1, Sub_id2, Sub_id3 (tracking parameters)
 * - Tổng hoa hồng sản phẩm(₫) (Commission amount)
 *
 * @author CashBee Team
 */
@Component
@Slf4j
public class ShopeeCSVParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // CSV column indices (0-based)
    private static final int COL_ORDER_ID = 0;
    private static final int COL_ORDER_STATUS = 1;
    private static final int COL_ORDER_TIME = 3;
    private static final int COL_SHOP_ID = 7;
    private static final int COL_ITEM_ID = 9;
    private static final int COL_ITEM_NAME = 10;
    private static final int COL_PRICE = 17;
    private static final int COL_QUANTITY = 18;
    private static final int COL_TOTAL_COMMISSION = 27;
    private static final int COL_SUB_ID1 = 40;
    private static final int COL_SUB_ID2 = 41;
    private static final int COL_SUB_ID3 = 42;

    /**
     * Parse Shopee CSV file and extract order data.
     *
     * @param inputStream CSV file input stream
     * @return List of parsed order records
     * @throws IOException if file reading fails
     */
    public List<ShopeeOrderRecord> parse(InputStream inputStream) throws IOException {
        List<ShopeeOrderRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // Skip BOM if present
            reader.mark(1);
            int firstChar = reader.read();
            if (firstChar != 0xFEFF) {
                reader.reset();  // No BOM, go back
            }

            // Read and skip header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty");
            }

            log.info("CSV header: {}", headerLine);

            // Read data lines
            String line;
            int rowNumber = 1;  // Row 1 is first data row (after header)

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (line.trim().isEmpty()) {
                    continue;  // Skip empty lines
                }

                try {
                    ShopeeOrderRecord record = parseLine(line, rowNumber);
                    records.add(record);
                } catch (Exception e) {
                    log.error("Failed to parse CSV row {}: {}", rowNumber, e.getMessage());
                    // Create error record
                    ShopeeOrderRecord errorRecord = ShopeeOrderRecord.builder()
                        .rowNumber(rowNumber)
                        .rawData(line)
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
     * Parse a single CSV line.
     *
     * @param line CSV line
     * @param rowNumber Row number (for error reporting)
     * @return Parsed order record
     */
    private ShopeeOrderRecord parseLine(String line, int rowNumber) {
        // Simple CSV parsing (split by comma)
        // Note: This doesn't handle quoted fields with commas inside
        // For production, use Apache Commons CSV or OpenCSV
        String[] columns = line.split(",", -1);  // -1 to keep trailing empty strings

        return ShopeeOrderRecord.builder()
            .rowNumber(rowNumber)
            .rawData(line)
            .orderId(getColumn(columns, COL_ORDER_ID))
            .orderStatus(getColumn(columns, COL_ORDER_STATUS))
            .orderTime(parseDateTime(getColumn(columns, COL_ORDER_TIME)))
            .shopId(getColumn(columns, COL_SHOP_ID))
            .itemId(getColumn(columns, COL_ITEM_ID))
            .itemName(getColumn(columns, COL_ITEM_NAME))
            .price(parseBigDecimal(getColumn(columns, COL_PRICE)))
            .quantity(parseInteger(getColumn(columns, COL_QUANTITY)))
            .totalCommission(parseBigDecimal(getColumn(columns, COL_TOTAL_COMMISSION)))
            .subId1(getColumn(columns, COL_SUB_ID1))
            .subId2(getColumn(columns, COL_SUB_ID2))
            .subId3(getColumn(columns, COL_SUB_ID3))
            .build();
    }

    /**
     * Get column value safely.
     */
    private String getColumn(String[] columns, int index) {
        if (index >= 0 && index < columns.length) {
            String value = columns[index].trim();
            return value.isEmpty() ? null : value;
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
            log.warn("Failed to parse datetime: {}", value);
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
            // Remove currency symbols and commas
            String cleaned = value.replace("₫", "")
                .replace(",", "")
                .replace(" ", "")
                .trim();

            if (cleaned.isEmpty()) {
                return null;
            }

            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal: {}", value);
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
            log.warn("Failed to parse Integer: {}", value);
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
         * Order timestamp.
         */
        private LocalDateTime orderTime;

        /**
         * Shop ID.
         */
        private String shopId;

        /**
         * Item ID (Product ID).
         */
        private String itemId;

        /**
         * Item name (Product name).
         */
        private String itemName;

        /**
         * Product price.
         */
        private BigDecimal price;

        /**
         * Quantity ordered.
         */
        private Integer quantity;

        /**
         * Total commission amount for this order.
         */
        private BigDecimal totalCommission;

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
    }
}
