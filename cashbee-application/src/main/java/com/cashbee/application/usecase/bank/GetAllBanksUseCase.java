package com.cashbee.application.usecase.bank;

import com.cashbee.application.dto.bank.BankResponse;
import com.cashbee.domain.model.Bank;
import com.cashbee.domain.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case: Get all active banks.
 *
 * Returns list of all active banks that users can select
 * when setting up their bank account information.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAllBanksUseCase {

    private final BankRepository bankRepository;

    /**
     * Execute use case to get all active banks.
     *
     * @return List of active banks
     */
    @Transactional(readOnly = true)
    public List<BankResponse> execute() {
        log.info("Getting all active banks");

        List<Bank> banks = bankRepository.findAllActive();

        log.info("Found {} active banks", banks.size());

        return banks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map Bank domain model to BankResponse DTO.
     *
     * @param bank Bank domain model
     * @return BankResponse DTO
     */
    private BankResponse mapToResponse(Bank bank) {
        return BankResponse.builder()
                .id(bank.getId())
                .bankCode(bank.getBankCode())
                .bankName(bank.getBankName())
                .isActive(bank.getIsActive())
                .build();
    }
}
