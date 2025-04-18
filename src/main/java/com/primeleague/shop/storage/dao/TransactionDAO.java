package com.primeleague.shop.storage.dao;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.utils.ShopConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * DAO para acesso a transações no banco de dados
 */
public class TransactionDAO {

  private final PrimeLeagueShopPlugin plugin;
  private Connection connection;

  /**
   * Cria um novo DAO
   *
   * @param plugin Instância do plugin
   * @throws SQLException Se houver erro ao conectar ao banco
   */
  public TransactionDAO(PrimeLeagueShopPlugin plugin) throws SQLException {
    this.plugin = plugin;
    this.connection = plugin.getConnection();
    createTable();
  }

  /**
   * Cria a tabela de transações se não existir
   *
   * @throws SQLException Se houver erro ao criar tabela
   */
  private void createTable() throws SQLException {
    String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
        "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
        "player_name VARCHAR(16) NOT NULL, " +
        "item_name VARCHAR(64) NOT NULL, " +
        "quantity INTEGER NOT NULL, " +
        "price DOUBLE NOT NULL, " +
        "type VARCHAR(4) NOT NULL, " +
        "timestamp BIGINT NOT NULL, " +
        "success BOOLEAN DEFAULT FALSE, " +
        "INDEX idx_player (player_name), " +
        "INDEX idx_timestamp (timestamp)" +
        ")";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.execute();
    }
  }

  /**
   * Salva uma transação no banco de dados
   *
   * @param transaction Transação a salvar
   * @throws SQLException Se houver erro ao salvar
   */
  public void saveTransaction(Transaction transaction) throws SQLException {
    String sql = "INSERT INTO transactions (player_name, item_name, quantity, price, type, timestamp, success) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, transaction.getPlayerName());
      stmt.setString(2, transaction.getItemName());
      stmt.setInt(3, transaction.getQuantity());
      stmt.setDouble(4, transaction.getPrice());
      stmt.setString(5, transaction.isBuy() ? "BUY" : "SELL");
      stmt.setLong(6, transaction.getTimestamp());
      stmt.setBoolean(7, transaction.isSuccessful());

      stmt.executeUpdate();
    }
  }

  public List<Transaction> getPlayerTransactions(String playerName, int limit) throws SQLException {
    String sql = "SELECT * FROM transactions WHERE player_name = ? ORDER BY timestamp DESC LIMIT ?";
    List<Transaction> transactions = new ArrayList<>();

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, playerName);
      stmt.setInt(2, limit);

      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        Transaction transaction = new Transaction(
            rs.getString("player_name"),
            rs.getString("item_name"),
            rs.getInt("quantity"),
            rs.getDouble("price"),
            rs.getLong("timestamp"),
            rs.getString("type").equals("BUY")
        );

        if (rs.getBoolean("success")) {
          transaction.markSuccessful();
        }

        transactions.add(transaction);
      }
    }

    return transactions;
  }

  public void cleanOldTransactions(long olderThan) throws SQLException {
    String sql = "DELETE FROM transactions WHERE timestamp < ?";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, olderThan);
      stmt.executeUpdate();
    }
  }

  /**
   * Fecha a conexão com o banco de dados
   */
  public void closeConnection() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "Erro ao fechar conexão com banco de dados", e);
    }
  }
}
