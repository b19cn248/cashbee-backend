package com.cashbee.application.dto.affiliate;

import com.cashbee.domain.model.AffiliateClick;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of fallback matching attempt when Sub_id1 is missing from CSV import.
 *
 * This DTO encapsulates the result of attempting to match an order
 * with a click using context (itemId, shopId, time window) instead
 * of the tracking code.
 *
 * Three possible outcomes:
 * 1. Unique match: Exactly one click found - safe to auto-assign
 * 2. Multiple matches: Multiple clicks found - needs manual review
 * 3. No match: No clicks found - order should be skipped
 *
 * @author CashBee Team
 */
@Getter
@Builder
public class FallbackMatchResult {

    /**
     * The matched click (if unique match found).
     * Null if multiple matches or no match.
     */
    private final AffiliateClick click;

    /**
     * All possible matching clicks (if any found).
     * Empty list if no matches.
     */
    private final List<AffiliateClick> possibleMatches;

    /**
     * Check if exactly one match was found (safe to auto-assign).
     *
     * @return true if exactly one click matches
     */
    public boolean isUniqueMatch() {
        return possibleMatches != null && possibleMatches.size() == 1;
    }

    /**
     * Check if multiple matches were found (needs manual review).
     *
     * @return true if more than one click matches
     */
    public boolean hasMultipleMatches() {
        return possibleMatches != null && possibleMatches.size() > 1;
    }

    /**
     * Check if no matches were found.
     *
     * @return true if no clicks match
     */
    public boolean hasNoMatch() {
        return possibleMatches == null || possibleMatches.isEmpty();
    }

    /**
     * Get count of possible matches.
     *
     * @return number of matching clicks
     */
    public int getMatchCount() {
        return possibleMatches != null ? possibleMatches.size() : 0;
    }

    /**
     * Get list of possible user IDs (for logging/review).
     *
     * @return distinct list of user IDs from matching clicks
     */
    public List<Long> getPossibleUserIds() {
        if (possibleMatches == null) {
            return List.of();
        }
        return possibleMatches.stream()
            .map(AffiliateClick::getUserId)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Create result for unique match.
     *
     * @param click the single matching click
     * @return FallbackMatchResult indicating unique match
     */
    public static FallbackMatchResult uniqueMatch(AffiliateClick click) {
        return FallbackMatchResult.builder()
            .click(click)
            .possibleMatches(List.of(click))
            .build();
    }

    /**
     * Create result for multiple matches.
     *
     * @param clicks list of matching clicks (more than one)
     * @return FallbackMatchResult indicating multiple matches need review
     */
    public static FallbackMatchResult multipleMatches(List<AffiliateClick> clicks) {
        return FallbackMatchResult.builder()
            .click(null)
            .possibleMatches(clicks)
            .build();
    }

    /**
     * Create result for no match.
     *
     * @return FallbackMatchResult indicating no matches found
     */
    public static FallbackMatchResult noMatch() {
        return FallbackMatchResult.builder()
            .click(null)
            .possibleMatches(List.of())
            .build();
    }
}
