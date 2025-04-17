package com.primeleague.shop.ranking;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.Transaction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RankingManager {
  private final PrimeLeagueShopPlugin plugin;
  private final Map<String, PlayerStats> weeklyStats;
  private long lastReset;

  private static class PlayerStats {
    double totalBought;
    double totalSold;
    int transactions;

    void addTransaction(Transaction transaction) {
      if (transaction.getType() == Transaction.TransactionType.BUY) {
        totalBought += transaction.getTotalPrice();
      } else {
        totalSold += transaction.getTotalPrice();
      }
      transactions++;
    }
  }

  public RankingManager(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.weeklyStats = new ConcurrentHashMap<>();
    this.lastReset = System.currentTimeMillis();
    startWeeklyReset();
  }

  private void startWeeklyReset() {
    // Reseta toda segunda-feira às 00:00
    plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
      @Override
      public void run() {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY &&
            cal.get(Calendar.HOUR_OF_DAY) == 0 &&
            cal.get(Calendar.MINUTE) == 0) {
          resetStats();
        }
      }
    }, 1200L, 1200L); // Checa a cada minuto
  }

  public void addTransaction(Transaction transaction) {
    String playerName = transaction.getPlayerName();
    weeklyStats.computeIfAbsent(playerName, k -> new PlayerStats())
        .addTransaction(transaction);
  }

  public void resetStats() {
    weeklyStats.clear();
    lastReset = System.currentTimeMillis();
    plugin.getLogger().info("Ranking semanal resetado!");
  }

  public List<Map.Entry<String, Double>> getTopBuyers(int limit) {
    List<Map.Entry<String, Double>> topBuyers = new ArrayList<>();
    for (Map.Entry<String, PlayerStats> entry : weeklyStats.entrySet()) {
      topBuyers.add(new AbstractMap.SimpleEntry<>(
          entry.getKey(), entry.getValue().totalBought));
    }

    Collections.sort(topBuyers, new Comparator<Map.Entry<String, Double>>() {
      @Override
      public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });

    return topBuyers.subList(0, Math.min(limit, topBuyers.size()));
  }

  public List<Map.Entry<String, Double>> getTopSellers(int limit) {
    List<Map.Entry<String, Double>> topSellers = new ArrayList<>();
    for (Map.Entry<String, PlayerStats> entry : weeklyStats.entrySet()) {
      topSellers.add(new AbstractMap.SimpleEntry<>(
          entry.getKey(), entry.getValue().totalSold));
    }

    Collections.sort(topSellers, new Comparator<Map.Entry<String, Double>>() {
      @Override
      public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });

    return topSellers.subList(0, Math.min(limit, topSellers.size()));
  }

  public PlayerStats getPlayerStats(String playerName) {
    return weeklyStats.get(playerName);
  }

  public long getLastReset() {
    return lastReset;
  }
}
