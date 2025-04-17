package com.primeleague.shop.services;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class RankingManager {
  private final PrimeLeagueShopPlugin plugin;
  private final Map<String, PlayerStats> statsCache;
  private final boolean useDatabase;
  private long lastReset;

  private static class PlayerStats {
    private double totalBought;
    private double totalSold;
    private int weekNumber;
    private int year;

    public PlayerStats() {
      this.totalBought = 0;
      this.totalSold = 0;
      Calendar cal = Calendar.getInstance();
      this.weekNumber = cal.get(Calendar.WEEK_OF_YEAR);
      this.year = cal.get(Calendar.YEAR);
    }
  }

  public RankingManager(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.statsCache = new HashMap<String, PlayerStats>();
    this.useDatabase = plugin.getConfigLoader().shouldLogToDatabase();
    this.lastReset = System.currentTimeMillis();

    if (useDatabase) {
      createTable();
      loadFromDatabase();
    }

    // Agenda reset semanal
    scheduleWeeklyReset();
  }

  public void updateStats(String playerName, double amount, boolean isBuy) {
    PlayerStats stats = statsCache.get(playerName);
    if (stats == null) {
      stats = new PlayerStats();
      statsCache.put(playerName, stats);
    }

    if (isBuy) {
      stats.totalBought += amount;
    } else {
      stats.totalSold += amount;
    }

    // Salva no banco assincronamente
    if (useDatabase) {
      final PlayerStats finalStats = stats;
      plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
        public void run() {
          saveToDatabase(playerName, finalStats);
        }
      });
    }
  }

  public List<Map.Entry<String, Double>> getTopBuyers(int limit) {
    List<Map.Entry<String, Double>> topList = new ArrayList<Map.Entry<String, Double>>();
    Map<String, Double> buyerMap = new HashMap<String, Double>();

    for (Map.Entry<String, PlayerStats> entry : statsCache.entrySet()) {
      buyerMap.put(entry.getKey(), entry.getValue().totalBought);
    }

    topList.addAll(buyerMap.entrySet());
    Collections.sort(topList, new Comparator<Map.Entry<String, Double>>() {
      public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });

    return topList.subList(0, Math.min(limit, topList.size()));
  }

  public List<Map.Entry<String, Double>> getTopSellers(int limit) {
    List<Map.Entry<String, Double>> topList = new ArrayList<Map.Entry<String, Double>>();
    Map<String, Double> sellerMap = new HashMap<String, Double>();

    for (Map.Entry<String, PlayerStats> entry : statsCache.entrySet()) {
      sellerMap.put(entry.getKey(), entry.getValue().totalSold);
    }

    topList.addAll(sellerMap.entrySet());
    Collections.sort(topList, new Comparator<Map.Entry<String, Double>>() {
      public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });

    return topList.subList(0, Math.min(limit, topList.size()));
  }

  private void createTable() {
    try {
      Connection conn = plugin.getConnection();
      PreparedStatement stmt = conn.prepareStatement(
          "CREATE TABLE IF NOT EXISTS shop_rankings (" +
              "player VARCHAR(16) NOT NULL, " +
              "total_bought DOUBLE NOT NULL, " +
              "total_sold DOUBLE NOT NULL, " +
              "week_number INT NOT NULL, " +
              "year INT NOT NULL, " +
              "PRIMARY KEY (player, week_number, year)" +
              ")");
      stmt.execute();
      stmt.close();
    } catch (SQLException e) {
      plugin.getLogger().severe("Erro ao criar tabela de rankings: " + e.getMessage());
    }
  }

  private void loadFromDatabase() {
    try {
      Connection conn = plugin.getConnection();
      Calendar cal = Calendar.getInstance();
      int currentWeek = cal.get(Calendar.WEEK_OF_YEAR);
      int currentYear = cal.get(Calendar.YEAR);

      PreparedStatement stmt = conn.prepareStatement(
          "SELECT * FROM shop_rankings WHERE week_number = ? AND year = ?");
      stmt.setInt(1, currentWeek);
      stmt.setInt(2, currentYear);

      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        String playerName = rs.getString("player");
        PlayerStats stats = new PlayerStats();
        stats.totalBought = rs.getDouble("total_bought");
        stats.totalSold = rs.getDouble("total_sold");
        stats.weekNumber = rs.getInt("week_number");
        stats.year = rs.getInt("year");

        statsCache.put(playerName, stats);
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      plugin.getLogger().severe("Erro ao carregar rankings: " + e.getMessage());
    }
  }

  private void saveToDatabase(String playerName, PlayerStats stats) {
    try {
      Connection conn = plugin.getConnection();
      PreparedStatement stmt = conn.prepareStatement(
          "REPLACE INTO shop_rankings (player, total_bought, total_sold, week_number, year) " +
              "VALUES (?, ?, ?, ?, ?)");

      stmt.setString(1, playerName);
      stmt.setDouble(2, stats.totalBought);
      stmt.setDouble(3, stats.totalSold);
      stmt.setInt(4, stats.weekNumber);
      stmt.setInt(5, stats.year);

      stmt.execute();
      stmt.close();
    } catch (SQLException e) {
      plugin.getLogger().severe("Erro ao salvar ranking: " + e.getMessage());
    }
  }

  private void scheduleWeeklyReset() {
    // Agenda para verificar reset a cada 6 horas
    plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
      public void run() {
        Calendar cal = Calendar.getInstance();
        int currentWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int currentYear = cal.get(Calendar.YEAR);

        boolean needsReset = false;
        for (PlayerStats stats : statsCache.values()) {
          if (stats.weekNumber != currentWeek || stats.year != currentYear) {
            needsReset = true;
            break;
          }
        }

        if (needsReset) {
          resetRankings();
        }
      }
    }, 432000L, 432000L); // 6 horas em ticks
  }

  public void resetRankings() {
    statsCache.clear();
    lastReset = System.currentTimeMillis();

    if (useDatabase) {
      plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
        public void run() {
          try {
            Connection conn = plugin.getConnection();
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM shop_rankings");
            stmt.execute();
            stmt.close();
          } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao limpar rankings: " + e.getMessage());
          }
        }
      });
    }

    plugin.getLogger().info("Rankings resetados!");
  }

  public long getLastReset() {
    return lastReset;
  }

  public void shutdown() {
    // Salva todos os dados pendentes
    if (useDatabase) {
      for (Map.Entry<String, PlayerStats> entry : statsCache.entrySet()) {
        saveToDatabase(entry.getKey(), entry.getValue());
      }
    }
    statsCache.clear();
  }
}
