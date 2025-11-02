package com.cashbee.application.usecase.transaction;

import com.cashbee.application.dto.transaction.CreateTransactionCommand;
import com.cashbee.application.dto.transaction.TransactionResponse;
import com.cashbee.application.port.TransactionMapper;
import com.cashbee.domain.model.Transaction;
import com.cashbee.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for creating transaction records.
 *
 * Business Scenario:
 * Every wallet operation (pending add, confirm, lock, unlock, deduct)
 * must create a transaction record for audit trail and compliance.
 *
 * Flow:
 * 1. Wallet use case performs operation
 * 2. Creates CreateTransactionCommand with operation details
 * 3. This use case saves transaction to database
 * 4. Returns transaction response
 *
 * This ensures:
 * - Complete audit trail
 * - Transaction history for users
 * - Compliance and reporting
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Execute create transaction operation.
     *
     * @param command Create transaction command
     * @return Created transaction response
     * @throws NullPointerException if command is null
     */
    @Transactional
    public TransactionResponse execute(CreateTransactionCommand command) {
        log.info("Creating transaction: userId={}, type={}, amount={}",
                command.getUserId(), command.getType(), command.getAmount());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("CreateTransactionCommand cannot be null");
        }

        // Step 2: Build domain model from command
        Transaction transaction = Transaction.builder()
                .userId(command.getUserId())
                .walletId(command.getWalletId())
                .type(command.getType())
                .amount(command.getAmount())
                .description(command.getDescription())
                .balanceBefore(command.getBalanceBefore())
                .balanceAfter(command.getBalanceAfter())
                .status(command.getStatus())
                .createdAt(LocalDateTime.now())
                .build();

        // Step 3: Validate domain model
        transaction.validate();

        // Step 4: Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Transaction created successfully: id={}, userId={}, type={}",
                savedTransaction.getId(),
                savedTransaction.getUserId(),
                savedTransaction.getType());

        // Step 5: Map to response DTO
        return transactionMapper.toResponse(savedTransaction);
    }
}
