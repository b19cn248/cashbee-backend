package com.cashbee.application.usecase.flashsale;

import com.cashbee.domain.flashsale.FlashSaleDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for retrieving Flash Sale time slots.
 *
 * Returns the list of available time slots via FlashSaleDataProvider port.
 * Time slots represent different flash sale sessions throughout the day.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetFlashSaleTimeSlotsUseCase {

    private final FlashSaleDataProvider flashSaleDataProvider;

    /**
     * Execute use case to get available time slots.
     *
     * @return List of time slot names (e.g., ["16-12 Khung: 00:00", ...])
     */
    @Transactional(readOnly = true)
    public List<String> execute() {
        log.info("GetFlashSaleTimeSlots: Fetching available time slots");

        List<String> timeSlots = flashSaleDataProvider.getTimeSlots();

        log.info("GetFlashSaleTimeSlots: Found {} time slots", timeSlots.size());
        return timeSlots;
    }
}
