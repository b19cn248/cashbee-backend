package com.cashbee.application.usecase.transaction;

import com.cashbee.application.dto.transaction.GetTransactionHistoryQuery;
import com.cashbee.application.dto.transaction.TransactionResponse;
import com.cashbee.application.port.TransactionMapper;
import com.cashbee.domain.model.Transaction;
import com.cashbee.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for getting transaction history.
 *
 * Business Scenario:
 * Users need to view their transaction history to track their earnings,
 * withdrawals, and other wallet activities.
 *
 * Flow:
 * 1. User requests transaction history (via API)
 * 2. System fetches transactions with pagination
 * 3. Returns list of transactions (newest first)
 *
 * This provides:
 * - Transparency for users
 * - Audit trail visibility
 * - Better user experience
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetTransactionHistoryUseCase {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Execute get transaction history operation.
     *
     * @param query Query containing userId and pagination params
     * @return List of transaction responses (paginated)
     * @throws NullPointerException if query is null
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> execute(GetTransactionHistoryQuery query) {
        log.info("Getting transaction history: userId={}, page={}, size={}",
                query.getUserId(), query.getPage(), query.getSize());

        // Step 1: Validate query (null check)
        if (query == null) {
            throw new NullPointerException("GetTransactionHistoryQuery cannot be null");
        }

        // Step 2: Fetch transactions with pagination (sorted by createdAt DESC)
        List<Transaction> transactions = transactionRepository.findByUserId(
                query.getUserId(),
                query.getPage(),
                query.getSize()
        );

        log.info("Found {} transactions for user {} (page {})",
                transactions.size(), query.getUserId(), query.getPage());

        // Step 3: Map to response DTOs
        return transactionMapper.toResponseList(transactions);
    }
}
