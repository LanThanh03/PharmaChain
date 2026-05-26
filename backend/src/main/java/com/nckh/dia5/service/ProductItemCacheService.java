package com.nckh.dia5.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nckh.dia5.model.ProductItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service để cache product item data
 * Sử dụng Redis để giảm database load
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductItemCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Cache key prefixes
    private static final String ITEM_KEY_PREFIX = "product_item:";
    private static final String ITEM_CODE_KEY_PREFIX = "item_code:";
    private static final String BATCH_ITEMS_KEY_PREFIX = "batch_items:";
    private static final String OWNER_STATS_KEY_PREFIX = "owner_stats:";
    private static final String VERIFICATION_RESULT_PREFIX = "verification:";

    // Cache TTL
    private static final long ITEM_TTL_SECONDS = 3600; // 1 hour
    private static final long BATCH_ITEMS_TTL_SECONDS = 1800; // 30 minutes
    private static final long STATS_TTL_SECONDS = 600; // 10 minutes
    private static final long VERIFICATION_TTL_SECONDS = 300; // 5 minutes

    /**
     * Cache product item by ID
     */
    public void cacheItem(ProductItem item) {
        try {
            String key = ITEM_KEY_PREFIX + item.getId();
            redisTemplate.opsForValue().set(key, item, ITEM_TTL_SECONDS, TimeUnit.SECONDS);
            
            // Also cache by item code for quick lookup
            String codeKey = ITEM_CODE_KEY_PREFIX + item.getItemCode();
            redisTemplate.opsForValue().set(codeKey, item.getId(), ITEM_TTL_SECONDS, TimeUnit.SECONDS);
            
            log.debug("Cached item: {} ({})", item.getId(), item.getItemCode());
        } catch (Exception e) {
            log.error("Failed to cache item", e);
        }
    }

    /**
     * Get cached item by ID
     */
    public ProductItem getCachedItem(Long itemId) {
        try {
            String key = ITEM_KEY_PREFIX + itemId;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ProductItem) {
                log.debug("Cache hit for item: {}", itemId);
                return (ProductItem) cached;
            }
        } catch (Exception e) {
            log.error("Failed to get cached item", e);
        }
        return null;
    }

    /**
     * Get cached item by code
     */
    public ProductItem getCachedItemByCode(String itemCode) {
        try {
            // First get item ID from code cache
            String codeKey = ITEM_CODE_KEY_PREFIX + itemCode;
            Object itemIdObj = redisTemplate.opsForValue().get(codeKey);
            
            if (itemIdObj instanceof Long) {
                Long itemId = (Long) itemIdObj;
                return getCachedItem(itemId);
            }
        } catch (Exception e) {
            log.error("Failed to get cached item by code", e);
        }
        return null;
    }

    /**
     * Cache batch items
     */
    public void cacheBatchItems(Long batchId, List<ProductItem> items) {
        try {
            String key = BATCH_ITEMS_KEY_PREFIX + batchId;
            redisTemplate.opsForValue().set(key, items, BATCH_ITEMS_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Cached {} items for batch: {}", items.size(), batchId);
        } catch (Exception e) {
            log.error("Failed to cache batch items", e);
        }
    }

    /**
     * Get cached batch items
     */
    @SuppressWarnings("unchecked")
    public List<ProductItem> getCachedBatchItems(Long batchId) {
        try {
            String key = BATCH_ITEMS_KEY_PREFIX + batchId;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                log.debug("Cache hit for batch items: {}", batchId);
                return (List<ProductItem>) cached;
            }
        } catch (Exception e) {
            log.error("Failed to get cached batch items", e);
        }
        return null;
    }

    /**
     * Cache owner statistics
     */
    public void cacheOwnerStats(Long ownerId, String ownerType, Map<String, Object> stats) {
        try {
            String key = OWNER_STATS_KEY_PREFIX + ownerId + ":" + ownerType;
            redisTemplate.opsForValue().set(key, stats, STATS_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Cached stats for owner: {} ({})", ownerId, ownerType);
        } catch (Exception e) {
            log.error("Failed to cache owner stats", e);
        }
    }

    /**
     * Get cached owner statistics
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedOwnerStats(Long ownerId, String ownerType) {
        try {
            String key = OWNER_STATS_KEY_PREFIX + ownerId + ":" + ownerType;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Map) {
                log.debug("Cache hit for owner stats: {} ({})", ownerId, ownerType);
                return (Map<String, Object>) cached;
            }
        } catch (Exception e) {
            log.error("Failed to get cached owner stats", e);
        }
        return null;
    }

    /**
     * Cache verification result
     */
    public void cacheVerificationResult(String itemCode, Map<String, Object> result) {
        try {
            String key = VERIFICATION_RESULT_PREFIX + itemCode;
            redisTemplate.opsForValue().set(key, result, VERIFICATION_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Cached verification result for: {}", itemCode);
        } catch (Exception e) {
            log.error("Failed to cache verification result", e);
        }
    }

    /**
     * Get cached verification result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedVerificationResult(String itemCode) {
        try {
            String key = VERIFICATION_RESULT_PREFIX + itemCode;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Map) {
                log.debug("Cache hit for verification: {}", itemCode);
                return (Map<String, Object>) cached;
            }
        } catch (Exception e) {
            log.error("Failed to get cached verification result", e);
        }
        return null;
    }

    /**
     * Invalidate item cache
     */
    public void invalidateItem(Long itemId, String itemCode) {
        try {
            redisTemplate.delete(ITEM_KEY_PREFIX + itemId);
            redisTemplate.delete(ITEM_CODE_KEY_PREFIX + itemCode);
            redisTemplate.delete(VERIFICATION_RESULT_PREFIX + itemCode);
            log.debug("Invalidated cache for item: {} ({})", itemId, itemCode);
        } catch (Exception e) {
            log.error("Failed to invalidate item cache", e);
        }
    }

    /**
     * Invalidate batch items cache
     */
    public void invalidateBatchItems(Long batchId) {
        try {
            redisTemplate.delete(BATCH_ITEMS_KEY_PREFIX + batchId);
            log.debug("Invalidated batch items cache: {}", batchId);
        } catch (Exception e) {
            log.error("Failed to invalidate batch items cache", e);
        }
    }

    /**
     * Invalidate owner stats cache
     */
    public void invalidateOwnerStats(Long ownerId, String ownerType) {
        try {
            redisTemplate.delete(OWNER_STATS_KEY_PREFIX + ownerId + ":" + ownerType);
            log.debug("Invalidated owner stats cache: {} ({})", ownerId, ownerType);
        } catch (Exception e) {
            log.error("Failed to invalidate owner stats cache", e);
        }
    }

    /**
     * Clear all product item caches
     */
    public void clearAllCaches() {
        try {
            redisTemplate.delete(redisTemplate.keys(ITEM_KEY_PREFIX + "*"));
            redisTemplate.delete(redisTemplate.keys(ITEM_CODE_KEY_PREFIX + "*"));
            redisTemplate.delete(redisTemplate.keys(BATCH_ITEMS_KEY_PREFIX + "*"));
            redisTemplate.delete(redisTemplate.keys(OWNER_STATS_KEY_PREFIX + "*"));
            redisTemplate.delete(redisTemplate.keys(VERIFICATION_RESULT_PREFIX + "*"));
            log.info("Cleared all product item caches");
        } catch (Exception e) {
            log.error("Failed to clear caches", e);
        }
    }

    /**
     * Warm up cache (pre-load frequently accessed data)
     */
    public void warmUpCache(List<ProductItem> items) {
        try {
            log.info("Warming up cache with {} items", items.size());
            for (ProductItem item : items) {
                cacheItem(item);
            }
        } catch (Exception e) {
            log.error("Failed to warm up cache", e);
        }
    }
}

