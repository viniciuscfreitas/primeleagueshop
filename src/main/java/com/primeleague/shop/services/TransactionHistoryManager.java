package com.primeleague.shop.services;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.ShopConstants;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;

public class TransactionHistoryManager {
  private final PrimeLeagueShopPlugin plugin;
  private final Map<String, List<Transaction>> memoryCache;
  private final boolean useDatabase;

  private static class Transaction {
    private final String playerName;
    private final String itemId;
    private final int quantity;
    private final double price;
    private final boolean isBuy;
    private final long timestamp;

    public Transaction(String playerName, String itemId, int quantity, double price, boolean isBuy) {
      this.playerName = playerName;
      this.itemId = itemId;
      this.quantity = quantity;
      this.price = price;
      this.isBuy = isBuy;
      this.timestamp = System.currentTimeMillis();
    }
  }

  public TransactionHistoryManager(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.memoryCache = new HashMap<>();
    this.useDatabase = plugin.isDatabaseEnabled();

    if (useDatabase) {
      createTable();
    }
  }

  public void recordTransaction(String playerName, ShopItem item, int quantity, double price, boolean isBuy) {
    final Transaction transaction = new Transaction(
        playerName,
        item.getMaterial().name() + ":" + item.getData(),
        quantity,
        price,
        isBuy);

    // Salva em memória
    List<Transaction> playerHistory = memoryCache.computeIfAbsent(playerName, k -> new ArrayList<>());
    playerHistory.add(0, transaction);

    // Limita o cache a 10 transações por jogador
    while (playerHistory.size() > 10) {
      playerHistory.remove(playerHistory.size() - 1);
    }

    // Salva no banco se disponível
    if (useDatabase) {
      plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveToDatabase(transaction));
    }
  }

  public List<Transaction> getPlayerHistory(String playerName) {
    return new ArrayList<>(memoryCache.getOrDefault(playerName, new ArrayList<>()));
  }

  private void createTable() {
    if (!useDatabase) {
      return;
    }

    try (Connection conn = plugin.getConnection();
        PreparedStatement stmt = conn.prepareStatement(
            "CREATE TABLE IF NOT EXISTS shop_transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "player VARCHAR(16) NOT NULL, " +
                "item_id VARCHAR(64) NOT NULL, " +
                "quantity INT NOT NULL, " +
                "price DOUBLE NOT NULL, " +
                "is_buy BOOLEAN NOT NULL, " +
                "timestamp DATETIME NOT NULL, " +
                "INDEX idx_player (player), " +
                "INDEX idx_timestamp (timestamp)" +
                ")")) {
      stmt.execute();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE,
          String.format(ShopConstants.LOG_DATABASE_ERROR, "Erro ao criar tabela de transações: " + e.getMessage()));
    }
  }

  private void saveToDatabase(Transaction transaction) {
    if (!useDatabase) {
      return;
    }

    try (Connection conn = plugin.getConnection();
        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO shop_transactions (player, item_id, quantity, price, is_buy, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)")) {

      stmt.setString(1, transaction.playerName);
      stmt.setString(2, transaction.itemId);
      stmt.setInt(3, transaction.quantity);
      stmt.setDouble(4, transaction.price);
      stmt.setBoolean(5, transaction.isBuy);
      stmt.setTimestamp(6, new Timestamp(transaction.timestamp));

      stmt.execute();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE,
          String.format(ShopConstants.LOG_DATABASE_ERROR, "Erro ao salvar transação: " + e.getMessage()));
    }
  }

  public void cleanOldRecords() {
    if (!useDatabase) {
      return;
    }

    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      try (Connection conn = plugin.getConnection();
          PreparedStatement stmt = conn.prepareStatement(
              "DELETE FROM shop_transactions WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY)")) {
        stmt.execute();
      } catch (SQLException e) {
        plugin.getLogger().log(Level.SEVERE,
            String.format(ShopConstants.LOG_DATABASE_ERROR, "Erro ao limpar registros antigos: " + e.getMessage()));
      }
    });
  }

  public void shutdown() {
    memoryCache.clear();
  }
}
