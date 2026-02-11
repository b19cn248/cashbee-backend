package com.cashbee.application.service.excel;

import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.BankTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Factory for creating appropriate Excel generator based on bank template type.
 *
 * This factory implements the Factory Pattern to:
 * 1. Encapsulate the logic of selecting the correct generator
 * 2. Make it easy to add new bank templates in the future
 * 3. Provide a single point of access for Excel generation
 *
 * Usage:
 * <pre>
 * BatchTransferExcelGenerator generator = factory.getGenerator(BankTemplate.VIETINBANK);
 * byte[] excelBytes = generator.generate(rows);
 * </pre>
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelGeneratorFactory {

    private final VPBankExcelGenerator vpBankGenerator;
    private final VietinBankExcelGenerator vietinBankGenerator;

    /**
     * Get the appropriate Excel generator for the given bank template.
     *
     * @param template The bank template type
     * @return The corresponding Excel generator
     * @throws BusinessException if template is not supported
     */
    public BatchTransferExcelGenerator getGenerator(BankTemplate template) {
        log.debug("ExcelGeneratorFactory: Getting generator for template: {}", template);

        if (template == null) {
            log.warn("ExcelGeneratorFactory: Template is null, defaulting to VPBANK");
            return vpBankGenerator;
        }

        return switch (template) {
            case VPBANK -> vpBankGenerator;
            case VIETINBANK -> vietinBankGenerator;
        };
    }

    /**
     * Get the appropriate Excel generator by template name string.
     *
     * This is useful when receiving template name from API request.
     *
     * @param templateName The bank template name (case-insensitive)
     * @return The corresponding Excel generator
     * @throws BusinessException if template name is invalid
     */
    public BatchTransferExcelGenerator getGenerator(String templateName) {
        log.debug("ExcelGeneratorFactory: Getting generator for template name: {}", templateName);

        if (templateName == null || templateName.isBlank()) {
            log.warn("ExcelGeneratorFactory: Template name is empty, defaulting to VPBANK");
            return vpBankGenerator;
        }

        try {
            BankTemplate template = BankTemplate.valueOf(templateName.toUpperCase());
            return getGenerator(template);
        } catch (IllegalArgumentException e) {
            log.error("ExcelGeneratorFactory: Invalid template name: {}", templateName);
            throw new BusinessException("INVALID_BANK_TEMPLATE",
                    "Invalid bank template: " + templateName + ". Supported values: VPBANK, VIETINBANK");
        }
    }
}
