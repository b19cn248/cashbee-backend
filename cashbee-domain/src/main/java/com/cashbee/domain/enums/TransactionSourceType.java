package com.cashbee.domain.enums;

/**
 * Enum representing the source/origin of a wallet transaction.
 *
 * This allows tracking where the money came from for audit and reporting.
 *
 * @author CashBee Team
 */
public enum TransactionSourceType {

    /**
     * Cashback from affiliate order.
     * source_id = cashback.id
     */
    ORDER,

    /**
     * Milestone bonus for referee (user completing orders).
     * source_id = referral_reward.id
     */
    MILESTONE_BONUS,

    /**
     * Bonus for referrer when referee reaches activation milestone.
     * source_id = referral_reward.id
     */
    REFERRER_BONUS,

    /**
     * Commission from referee's orders (5% for 5 months).
     * source_id = referrer_commission.id
     */
    REFERRER_COMMISSION,

    /**
     * Payout/withdrawal to bank account.
     * source_id = payout_request.id or batch_transfer_item.id
     */
    PAYOUT,

    /**
     * Manual adjustment by admin.
     * source_id = null or adjustment record id
     */
    ADJUSTMENT,

    /**
     * Refund for cancelled order.
     * source_id = order.id or cashback.id
     */
    REFUND
}
