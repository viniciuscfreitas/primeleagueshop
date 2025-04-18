package com.primeleague.shop.storage;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class TransactionHistory {
  private final PrimeLeagueShopPlugin plugin;
  private final String dbFile;
  private Connection connection;
  private boolean initialized;

  public TransactionHistory(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.dbFile = "jdbc:sqlite:" + plugin.getDataFolder().getPath() + "/transactions.db";
    this.initialized = false;

    // Tenta inicializar o banco de dados de forma assíncrona
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        initializeDatabase();
        initialized = true;
        plugin.getLogger().info("Histórico de transações inicializado com sucesso!");
      } catch (Exception e) {
        plugin.getLogger().severe("Erro ao inicializar histórico de transações: " + e.getMessage());
      }
    });
  }

  public boolean isInitialized() {
    return initialized;
  }

  public void initializeDatabase() {
    try {
      // Verifica se o ShopManager está disponível
      if (plugin.getShopManager() == null) {
        plugin.getLogger().severe("ShopManager não está disponível. Inicialização do histórico de transações abortada.");
        return;
      }

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
            boolean isBuy = rs.getString("type").equals("BUY");
            long timestamp = rs.getLong("timestamp");

            Transaction transaction = new Transaction(
              playerName,
              itemName,
              quantity,
              price,
              timestamp,
              isBuy
            );
            existingTransactions.add(transaction);
          } catch (Exception e) {
            plugin.getLogger().warning("Erro ao ler transação existente: " + e.getMessage());
            plugin.getLogger().warning("Detalhes do erro: " + e.toString());
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
        String insertSql = "INSERT INTO transactions (player_name, item_name, quantity, price, type, timestamp, success) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = connection.prepareStatement(insertSql);
        for (Transaction transaction : existingTransactions) {
          pstmt.setString(1, transaction.getPlayerName());
          pstmt.setString(2, transaction.getItemName());
          pstmt.setInt(3, transaction.getQuantity());
          pstmt.setDouble(4, transaction.getPrice());
          pstmt.setString(5, transaction.isBuy() ? "BUY" : "SELL");
          pstmt.setLong(6, transaction.getTimestamp());
          pstmt.setBoolean(7, transaction.isSuccessful());

          try {
            pstmt.executeUpdate();
          } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao reinserir transação: " + e.getMessage());
          }
        }
        pstmt.close();
      }

      stmt.close();
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Erro ao inicializar banco de dados", e);
    }
  }

  public void addTransaction(Transaction transaction) {
    if (!initialized) {
        plugin.getLogger().warning("Tentativa de adicionar transação antes da inicialização do banco de dados");
        return;
    }

    try {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbFile);
        }

        String sql = "INSERT INTO transactions (player_name, item_name, quantity, price, type, timestamp, success) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, transaction.getPlayerName());
        stmt.setString(2, transaction.getItemName());
        stmt.setInt(3, transaction.getQuantity());
        stmt.setDouble(4, transaction.getPrice());
        stmt.setString(5, transaction.isBuy() ? "BUY" : "SELL");
        stmt.setLong(6, transaction.getTimestamp());
        stmt.setBoolean(7, transaction.isSuccessful());

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

    if (!initialized) {
        plugin.getLogger().warning("Tentativa de acessar histórico antes da inicialização do banco de dados");
        return history;
    }

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
                String itemName = rs.getString("item_name");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                boolean isBuy = rs.getString("type").equals("BUY");
                long timestamp = rs.getLong("timestamp");

                Transaction transaction = new Transaction(
                    playerName,
                    itemName,
                    quantity,
                    price,
                    timestamp,
                    isBuy
                );

                if (rs.getBoolean("success")) {
                    transaction.markSuccessful();
                }

                history.add(transaction);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao ler transação: " + e.getMessage());
            }
        }

        rs.close();
        stmt.close();
    } catch (SQLException e) {
        plugin.getLogger().log(Level.WARNING, "Erro ao buscar histórico", e);
    }

    return history;
  }

  public void cleanup() {
    try {
      if (connection != null && !connection.isClosed()) {
        // Remove transações antigas (mais de 30 dias)
        PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM transactions WHERE timestamp < ?");
        stmt.setLong(1, System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L));
        stmt.executeUpdate();
        stmt.close();
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "Erro ao limpar transações antigas", e);
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

  public void openHistory(Player player) {
    List<Transaction> transactions = getPlayerHistory(player.getName(), 45);

    Inventory inv = Bukkit.createInventory(null, 54, TextUtils.colorize("&8Histórico de Transações"));

    int slot = 0;
    for (Transaction transaction : transactions) {
      ItemStack item = new ItemStack(Material.PAPER);
      ItemMeta meta = item.getItemMeta();

      String type = transaction.isBuy() ? "&aCompra" : "&cVenda";
      meta.setDisplayName(TextUtils.colorize(type + " - " + transaction.getItemName()));

      List<String> lore = new ArrayList<>();
      lore.add(TextUtils.colorize("&7Quantidade: &f" + transaction.getQuantity()));
      lore.add(TextUtils.colorize("&7Preço: &f$" + transaction.getPrice()));
      lore.add(TextUtils.colorize("&7Data: &f" + new java.util.Date(transaction.getTimestamp())));

      meta.setLore(lore);
      item.setItemMeta(meta);

      inv.setItem(slot++, item);
    }

    player.openInventory(inv);
  }
}
