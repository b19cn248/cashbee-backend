package com.cashbee.infrastructure.adapter;

import com.cashbee.domain.enums.ClickStatus;
import com.cashbee.domain.model.AffiliateClick;
import com.cashbee.domain.repository.AffiliateClickRepository;
import com.cashbee.infrastructure.mapper.AffiliateClickMapper;
import com.cashbee.infrastructure.repository.AffiliateClickJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Adapter for AffiliateClickRepository.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AffiliateClickRepositoryAdapter implements AffiliateClickRepository {

  private final AffiliateClickJpaRepository jpaRepository;
  private final AffiliateClickMapper mapper;

  @Override
  public AffiliateClick save(AffiliateClick click) {
    var entity = mapper.toEntity(click);
    var savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<AffiliateClick> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<AffiliateClick> findByTrackingCode(String trackingCode) {
    return jpaRepository.findByTrackingCode(trackingCode).map(mapper::toDomain);
  }

  @Override
  public List<AffiliateClick> findByUserId(Long userId) {
    return jpaRepository.findByUserId(userId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<AffiliateClick> findByUserIdAndPlatformId(Long userId, Long platformId) {
    return jpaRepository.findByUserIdAndPlatformId(userId, platformId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<AffiliateClick> findByStatus(ClickStatus status) {
    return jpaRepository.findByStatus(status).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<AffiliateClick> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
    return jpaRepository.findByCreatedAtBetween(start, end).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<AffiliateClick> findExpiredClicks(LocalDateTime cutoffDate) {
    return jpaRepository.findExpiredClicks(cutoffDate).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long countByUserId(Long userId) {
    return jpaRepository.countByUserId(userId);
  }

  @Override
  public long countByOrderMatchedTrue() {
    return jpaRepository.countByOrderMatchedTrue();
  }

  @Override
  public double getConversionRate() {
    Double rate = jpaRepository.calculateConversionRate();
    return rate != null ? rate : 0.0;
  }

  @Override
  public void deleteById(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public List<AffiliateClick> findPossibleMatchesByContext(
      Long platformId,
      String itemId,
      String shopId,
      LocalDateTime orderTime,
      int windowMinutes) {

    // Calculate window start time
    LocalDateTime windowStart = orderTime.minusMinutes(windowMinutes);

    log.debug("[ADAPTER] findPossibleMatchesByContext - platformId={}, itemId='{}', shopId='{}', orderTime={}, windowStart={}",
        platformId, itemId, shopId, orderTime, windowStart);

    // Find clicks matching context
    var entities = jpaRepository.findPossibleMatchesByContext(
        platformId,
        itemId,
        shopId,
        orderTime,
        windowStart
    );

    log.debug("[ADAPTER] findPossibleMatchesByContext returned {} entities", entities.size());

    // Log entity details if found
    for (var entity : entities) {
      log.debug("[ADAPTER] Found click: id={}, userId={}, itemId='{}', shopId='{}', createdAt={}, orderMatched={}",
          entity.getId(), entity.getUserId(), entity.getItemId(), entity.getShopId(),
          entity.getCreatedAt(), entity.getOrderMatched());
    }

    return entities.stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<AffiliateClick> findPossibleMatchesByItemAndShop(
      Long platformId,
      String itemId,
      String shopId) {

    log.debug("[ADAPTER] findPossibleMatchesByItemAndShop - platformId={}, itemId='{}', shopId='{}'",
        platformId, itemId, shopId);

    // Find clicks matching by itemId + shopId only (no time restriction)
    var entities = jpaRepository.findPossibleMatchesByItemAndShop(
        platformId,
        itemId,
        shopId
    );

    log.debug("[ADAPTER] findPossibleMatchesByItemAndShop returned {} entities", entities.size());

    // Log entity details if found
    for (var entity : entities) {
      log.debug("[ADAPTER] Found click: id={}, userId={}, itemId='{}', shopId='{}', createdAt={}, orderMatched={}",
          entity.getId(), entity.getUserId(), entity.getItemId(), entity.getShopId(),
          entity.getCreatedAt(), entity.getOrderMatched());
    }

    return entities.stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<AffiliateClick> findAllWithFilters(
      Long userId,
      Long platformId,
      ClickStatus status,
      Boolean orderMatched,
      String search,
      int page,
      int size) {

    log.debug("[ADAPTER] findAllWithFilters - userId={}, platformId={}, status={}, orderMatched={}, search='{}', page={}, size={}",
        userId, platformId, status, orderMatched, search, page, size);

    var pageable = PageRequest.of(page, size);
    var entities = jpaRepository.findAllWithFilters(
        userId, platformId, status, orderMatched, search, pageable);

    log.debug("[ADAPTER] findAllWithFilters returned {} entities", entities.size());

    return entities.stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long countWithFilters(
      Long userId,
      Long platformId,
      ClickStatus status,
      Boolean orderMatched,
      String search) {

    return jpaRepository.countWithFilters(userId, platformId, status, orderMatched, search);
  }
}
