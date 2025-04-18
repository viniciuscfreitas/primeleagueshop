package com.primeleague.shop.storage;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.models.ShopItem;
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
      if (connection == null || connection.isClosed()) {
        connection = DriverManager.getConnection(dbFile);
      }

      Statement stmt = connection.createStatement();

      // Primeiro fazemos backup dos dados existentes
      List<Transaction> existingTransactions = new ArrayList<>();
      try {
        ResultSet rs = stmt.executeQuery("SELECT * FROM transactions");
        while (rs.next()) {
          try {
            String playerName = rs.getString("player_name");
            String itemName = rs.getString("item_name");
            int quantity = rs.getInt("quantity");
            double price = rs.getDouble("price");
            String type = rs.getString("type");
            long timestamp = rs.getLong("timestamp");

            // Tenta ler material e data se existirem
            String material = null;
            byte data = 0;
            try {
              material = rs.getString("item_material");
              data = rs.getByte("item_data");
              plugin.getLogger().info("[Debug] Lendo transação - Material: " + material + ", Data: " + data);
            } catch (SQLException e) {
              plugin.getLogger().info("[Debug] Colunas item_material/item_data não encontradas, usando valores padrão");
            }

            // Tenta encontrar o item
            ShopItem item = null;
            if (material != null) {
              plugin.getLogger().info("[Debug] Tentando encontrar item por material: " + material);
              item = plugin.getShopManager().getItemByMaterialAndData(material, data);
              if (item == null) {
                plugin.getLogger().info("[Debug] Item não encontrado por material, tentando por nome: " + itemName);
                item = plugin.getShopManager().findItemByName(itemName);
              }
            } else {
              plugin.getLogger().info("[Debug] Material nulo, tentando encontrar por nome: " + itemName);
              item = plugin.getShopManager().findItemByName(itemName);
            }

            if (item != null) {
              plugin.getLogger().info("[Debug] Item encontrado: " + item.getName());
              Transaction transaction = new Transaction(
                playerName,
                item,
                quantity,
                price,
                Transaction.TransactionType.valueOf(type),
                new Timestamp(timestamp)
              );
              existingTransactions.add(transaction);
            } else {
              plugin.getLogger().warning("[Debug] Item não encontrado para transação - Nome: " + itemName + ", Material: " + material);
            }
          } catch (Exception e) {
            plugin.getLogger().warning("Erro ao ler transação existente: " + e.getMessage());
            plugin.getLogger().warning("Detalhes do erro: " + e.toString());
            e.printStackTrace();
          }
        }
        rs.close();
      } catch (SQLException e) {
        plugin.getLogger().warning("Tabela antiga não encontrada, criando nova");
      }

      // Dropa a tabela antiga
      stmt.execute("DROP TABLE IF EXISTS transactions");

      // Cria a nova tabela com todas as colunas
      stmt.execute(
          "CREATE TABLE transactions (" +
              "id INTEGER PRIMARY KEY AUTOINCREMENT," +
              "player_name TEXT NOT NULL," +
              "item_material TEXT NOT NULL," +
              "item_data INTEGER NOT NULL," +
              "item_name TEXT NOT NULL," +
              "quantity INTEGER NOT NULL," +
              "price REAL NOT NULL," +
              "type TEXT NOT NULL," +
              "timestamp BIGINT NOT NULL," +
              "success BOOLEAN DEFAULT 0" +
              ")"
      );

      // Recria o índice
      stmt.execute(
          "CREATE INDEX idx_player_timestamp ON transactions (player_name, timestamp)");

      // Reinsere os dados antigos
      if (!existingTransactions.isEmpty()) {
        String insertSql = "INSERT INTO transactions (player_name, item_material, item_data, item_name, quantity, price, type, timestamp, success) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = connection.prepareStatement(insertSql);
        for (Transaction transaction : existingTransactions) {
          ShopItem item = transaction.getItem();
          pstmt.setString(1, transaction.getPlayerName());
          pstmt.setString(2, item.getMaterial().name());
          pstmt.setInt(3, item.getData());
          pstmt.setString(4, item.getName());
          pstmt.setInt(5, transaction.getQuantity());
          pstmt.setDouble(6, transaction.getPrice());
          pstmt.setString(7, transaction.getType().toString());
          pstmt.setLong(8, transaction.getTimestamp().getTime());
          pstmt.setBoolean(9, transaction.isSuccessful());

          try {
            pstmt.executeUpdate();
          } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao reinserir transação: " + e.getMessage());
          }
        }
        pstmt.close();
      }

      stmt.close();
      plugin.getLogger().info("Banco de dados inicializado com sucesso!");
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Erro ao inicializar banco de dados", e);
    }
  }

  public void addTransaction(Transaction transaction) {
    try {
      if (connection == null || connection.isClosed()) {
        connection = DriverManager.getConnection(dbFile);
      }

      String sql = "INSERT INTO transactions (player_name, item_material, item_data, item_name, quantity, price, type, timestamp, success) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

      PreparedStatement stmt = connection.prepareStatement(sql);
      ShopItem item = transaction.getItem();
      stmt.setString(1, transaction.getPlayerName());
      stmt.setString(2, item.getMaterial().name());
      stmt.setInt(3, item.getData());
      stmt.setString(4, item.getName());
      stmt.setInt(5, transaction.getQuantity());
      stmt.setDouble(6, transaction.getPrice());
      stmt.setString(7, transaction.getType().toString());
      stmt.setLong(8, System.currentTimeMillis());
      stmt.setBoolean(9, transaction.isSuccessful());

      stmt.executeUpdate();
      stmt.close();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "Erro ao salvar transação", e);
      // Se falhou, tenta reinicializar o banco
      initializeDatabase();
    }
  }

  public List<Transaction> getPlayerHistory(String playerName) {
    return getPlayerHistory(playerName, 50); // Valor padrão de 50 registros
  }

  public List<Transaction> getPlayerHistory(String playerName, int limit) {
    List<Transaction> history = new ArrayList<>();

    try {
      if (connection == null || connection.isClosed()) {
        connection = DriverManager.getConnection(dbFile);
      }

      PreparedStatement stmt = connection.prepareStatement(
          "SELECT * FROM transactions WHERE player_name = ? ORDER BY timestamp DESC LIMIT ?");

      stmt.setString(1, playerName);
      stmt.setInt(2, limit);
      ResultSet rs = stmt.executeQuery();

      while (rs.next()) {
        try {
          String itemMaterial = rs.getString("item_material");
          if (itemMaterial == null) {
            plugin.getLogger().warning("Material nulo encontrado para transação ID: " + rs.getInt("id"));
            continue;
          }

          byte itemData = rs.getByte("item_data");
          ShopItem item = plugin.getShopManager().getItemByMaterialAndData(itemMaterial, itemData);

          if (item == null) {
            plugin.getLogger().warning("Item não encontrado para material: " + itemMaterial + " com data: " + itemData);
            continue;
          }

          Transaction transaction = new Transaction(
              rs.getString("player_name"),
              item,
              rs.getInt("quantity"),
              rs.getDouble("price"),
              Transaction.TransactionType.valueOf(rs.getString("type")),
              new Timestamp(rs.getLong("timestamp"))
          );

          try {
            transaction.setSuccess(rs.getBoolean("success"));
          } catch (SQLException e) {
            // Se a coluna success não existir, assume false
            transaction.setSuccess(false);
          }

          history.add(transaction);
        } catch (Exception e) {
          plugin.getLogger().warning("Erro ao processar transação: " + e.getMessage());
          continue;
        }
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      plugin.getLogger().severe("Erro ao carregar histórico de transações: " + e.getMessage());
      e.printStackTrace();
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

  public void saveTransaction(Transaction transaction) throws SQLException {
    if (connection == null || connection.isClosed()) {
      connection = DriverManager.getConnection(dbFile);
    }

    String sql = "INSERT INTO transactions (player_name, item_material, item_data, item_name, quantity, price, type, timestamp, success) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      ShopItem item = transaction.getItem();
      stmt.setString(1, transaction.getPlayerName());
      stmt.setString(2, item.getMaterial().name());
      stmt.setInt(3, item.getData());
      stmt.setString(4, item.getName());
      stmt.setInt(5, transaction.getQuantity());
      stmt.setDouble(6, transaction.getUnitPrice());
      stmt.setString(7, transaction.getType().name());
      stmt.setLong(8, System.currentTimeMillis());
      stmt.setBoolean(9, transaction.isSuccessful());

      stmt.executeUpdate();
    }
  }
}
