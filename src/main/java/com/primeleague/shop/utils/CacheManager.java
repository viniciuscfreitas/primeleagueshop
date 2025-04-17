package com.primeleague.shop.utils;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class CacheManager<K, V> {
  private final Map<K, CacheEntry<V>> cache;
  private final long expirationMs;

  private static class CacheEntry<V> {
    final V value;
    final long timestamp;

    CacheEntry(V value) {
      this.value = value;
      this.timestamp = System.currentTimeMillis();
    }

    boolean isExpired(long expirationMs) {
      return System.currentTimeMillis() - timestamp > expirationMs;
    }
  }

  public CacheManager(long expirationMinutes) {
    this.cache = Collections.synchronizedMap(new HashMap<K, CacheEntry<V>>());
    this.expirationMs = expirationMinutes * 60 * 1000;
  }

  public void put(K key, V value) {
    cache.put(key, new CacheEntry<>(value));
  }

  public V get(K key) {
    CacheEntry<V> entry = cache.get(key);
    if (entry == null)
      return null;
    if (entry.isExpired(expirationMs)) {
      cache.remove(key);
      return null;
    }
    return entry.value;
  }

  public void remove(K key) {
    cache.remove(key);
  }

  public void cleanup() {
    synchronized (cache) {
      Iterator<Map.Entry<K, CacheEntry<V>>> it = cache.entrySet().iterator();
      while (it.hasNext()) {
        if (it.next().getValue().isExpired(expirationMs)) {
          it.remove();
        }
      }
    }
  }
}
