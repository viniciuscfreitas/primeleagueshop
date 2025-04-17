package com.primeleague.shop.utils;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

public class RateLimiter {
  private final Map<String, TransactionCounter> counters;
  private final int maxTransactionsPerMinute;

  private static class TransactionCounter {
    int count;
    long lastReset;

    TransactionCounter() {
      this.count = 0;
      this.lastReset = System.currentTimeMillis();
    }

    void increment() {
      long now = System.currentTimeMillis();
      if (now - lastReset > 60000) { // 1 minuto
        count = 1;
        lastReset = now;
      } else {
        count++;
      }
    }

    boolean shouldAllow(int maxTransactions) {
      long now = System.currentTimeMillis();
      if (now - lastReset > 60000) {
        count = 0;
        lastReset = now;
        return true;
      }
      return count < maxTransactions;
    }
  }

  public RateLimiter(int maxTransactionsPerMinute) {
    this.counters = Collections.synchronizedMap(new HashMap<String, TransactionCounter>());
    this.maxTransactionsPerMinute = maxTransactionsPerMinute;
  }

  public boolean tryAcquire(String playerName) {
    TransactionCounter counter = counters.computeIfAbsent(playerName, k -> new TransactionCounter());
    synchronized (counter) {
      if (counter.shouldAllow(maxTransactionsPerMinute)) {
        counter.increment();
        return true;
      }
      return false;
    }
  }

  public void cleanup() {
    long now = System.currentTimeMillis();
    synchronized (counters) {
      counters.entrySet().removeIf(entry -> now - entry.getValue().lastReset > 300000); // Remove após 5 minutos de
                                                                                        // inatividade
    }
  }
}
