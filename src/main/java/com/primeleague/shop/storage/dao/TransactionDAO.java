package com.primeleague.shop.storage.dao;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.utils.ShopConstants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
    this.connection = openConnection();

    // Cria a tabela se não existir
    createTable();
  }

  /**
   * Abre uma conexão com o banco de dados
   *
   * @return Conexão aberta
   * @throws SQLException Se houver erro ao conectar
   */
  private Connection openConnection() throws SQLException {
    String host = plugin.getConfig().getString("database.host", "localhost");
    int port = plugin.getConfig().getInt("database.port", 3306);
    String database = plugin.getConfig().getString("database.database", "minecraft");
    String username = plugin.getConfig().getString("database.username", "root");
    String password = plugin.getConfig().getString("database.password", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
        "?useSSL=false&autoReconnect=true";

    Connection conn = DriverManager.getConnection(url, username, password);

    plugin.getLogger().info(ShopConstants.LOG_DATABASE_CONNECT);
    return conn;
  }

  /**
   * Fecha a conexão com o banco de dados
   */
  public void closeConnection() {
    if (connection != null) {
      try {
        if (!connection.isClosed()) {
          connection.close();
        }
      } catch (SQLException e) {
        plugin.getLogger().log(Level.WARNING,
            String.format(ShopConstants.LOG_DATABASE_ERROR, "Erro ao fechar conexão"), e);
      }
    }
  }

  /**
   * Reconecta ao banco de dados se necessário
   *
   * @throws SQLException Se houver erro ao reconectar
   */
  private void reconnectIfNeeded() throws SQLException {
    int attempts = 0;
    int maxAttempts = plugin.getConfig().getInt("error-handling.database.reconnect-attempts", 3);

    while (attempts < maxAttempts) {
      try {
        if (connection == null || connection.isClosed()) {
          connection = openConnection();
          plugin.getLogger().info(ShopConstants.LOG_DATABASE_RECONNECTED);
          return;
        }
        break;
      } catch (SQLException e) {
        attempts++;
        plugin.getLogger().warning(String.format(ShopConstants.LOG_DATABASE_RECONNECT, attempts));

        if (attempts >= maxAttempts) {
          throw e;
        }

        try {
          // Espera antes de tentar novamente
          Thread.sleep(1000);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Cria a tabela de transações se não existir
   *
   * @throws SQLException Se houver erro ao criar tabela
   */
  private void createTable() throws SQLException {
    String tablePrefix = plugin.getConfig().getString("database.table-prefix", "pl_");
    String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "shop_transactions (" +
        "id INT AUTO_INCREMENT PRIMARY KEY, " +
        "player_name VARCHAR(16) NOT NULL, " +
        "item_name VARCHAR(64) NOT NULL, " +
        "quantity INT NOT NULL, " +
        "price_per_unit DOUBLE NOT NULL, " +
        "total_price DOUBLE NOT NULL, " +
        "transaction_type ENUM('BUY', 'SELL') NOT NULL, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "INDEX idx_player (player_name)" +
        ")";

    try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
      statement.executeUpdate();
    }
  }

  /**
   * Salva uma transação no banco de dados
   *
   * @param transaction Transação a salvar
   * @throws SQLException Se houver erro ao salvar
   */
  public void saveTransaction(Transaction transaction) throws SQLException {
    if (!transaction.isSuccessful()) {
      return; // Não registra transações que não foram concluídas
    }

    reconnectIfNeeded();

    String tablePrefix = plugin.getConfig().getString("database.table-prefix", "pl_");
    String insertSQL = "INSERT INTO " + tablePrefix + "shop_transactions " +
        "(player_name, item_name, quantity, price_per_unit, total_price, transaction_type) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
      statement.setString(1, transaction.getPlayerName());
      statement.setString(2, transaction.getItem().getName());
      statement.setInt(3, transaction.getQuantity());
      statement.setDouble(4, transaction.getUnitPrice());
      statement.setDouble(5, transaction.getTotalPrice());
      statement.setString(6, transaction.getType().name());

      statement.executeUpdate();
    }
  }
}
