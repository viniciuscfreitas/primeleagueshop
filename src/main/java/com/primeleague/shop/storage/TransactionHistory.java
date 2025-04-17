package com.primeleague.shop.storage;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.Transaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class TransactionHistory {
  private final PrimeLeagueShopPlugin plugin;
  private final String dbFile;
  private Connection connection;

  public TransactionHistory(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.dbFile = "jdbc:sqlite:" + plugin.getDataFolder().getPath() + "/transactions.db";
    initializeDatabase();
  }

  private void initializeDatabase() {
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection(dbFile);

      Statement stmt = connection.createStatement();
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS transactions (" +
              "id INTEGER PRIMARY KEY AUTOINCREMENT," +
              "player_name TEXT NOT NULL," +
              "item_name TEXT NOT NULL," +
              "quantity INTEGER NOT NULL," +
              "price REAL NOT NULL," +
              "type TEXT NOT NULL," +
              "timestamp BIGINT NOT NULL" +
              ")");

      stmt.execute(
          "CREATE INDEX IF NOT EXISTS idx_player_timestamp ON transactions (player_name, timestamp)");

      stmt.close();
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Erro ao inicializar banco de dados", e);
    }
  }

  public void addTransaction(Transaction transaction) {
    try {
      PreparedStatement stmt = connection.prepareStatement(
          "INSERT INTO transactions (player_name, item_name, quantity, price, type, timestamp) " +
              "VALUES (?, ?, ?, ?, ?, ?)");

      stmt.setString(1, transaction.getPlayerName());
      stmt.setString(2, transaction.getItem().getName());
      stmt.setInt(3, transaction.getQuantity());
      stmt.setDouble(4, transaction.getTotalPrice());
      stmt.setString(5, transaction.getType().toString());
      stmt.setLong(6, System.currentTimeMillis());

      stmt.executeUpdate();
      stmt.close();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "Erro ao salvar transação", e);
    }
  }

  public List<Transaction> getPlayerHistory(String playerName, int limit) {
    List<Transaction> history = new ArrayList<>();
    try {
      PreparedStatement stmt = connection.prepareStatement(
          "SELECT * FROM transactions WHERE player_name = ? " +
              "ORDER BY timestamp DESC LIMIT ?");

      stmt.setString(1, playerName);
      stmt.setInt(2, limit);

      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        // TODO: Implementar conversão do ResultSet para Transaction
        // Precisa recuperar ShopItem do ShopManager
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "Erro ao carregar histórico", e);
    }
    return history;
  }

  public void cleanup() {
    try {
      // Remove transações antigas (mais de 30 dias)
      PreparedStatement stmt = connection.prepareStatement(
          "DELETE FROM transactions WHERE timestamp < ?");
      stmt.setLong(1, System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L));
      stmt.executeUpdate();
      stmt.close();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "Erro na limpeza do histórico", e);
    }
  }

  public void close() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "Erro ao fechar conexão", e);
    }
  }
}
