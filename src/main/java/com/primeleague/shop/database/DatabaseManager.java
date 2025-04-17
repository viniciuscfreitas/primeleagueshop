package com.primeleague.shop.database;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.utils.ShopConstants;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {
  private final PrimeLeagueShopPlugin plugin;
  private HikariDataSource dataSource;
  private final boolean enabled;

  public DatabaseManager(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.enabled = plugin.getConfig().getBoolean("settings.transaction.log-to-database", false);

    if (enabled) {
      setupPool();
    }
  }

  private void setupPool() {
    if (!enabled) {
      return;
    }

    try {
      HikariConfig config = new HikariConfig();

      String host = plugin.getConfig().getString("database.host", "localhost");
      int port = plugin.getConfig().getInt("database.port", 3306);
      String database = plugin.getConfig().getString("database.database", "minecraft");
      String username = plugin.getConfig().getString("database.username", "root");
      String password = plugin.getConfig().getString("database.password", "");

      config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s", host, port, database));
      config.setUsername(username);
      config.setPassword(password);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30));
      config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
      config.setMaxLifetime(TimeUnit.HOURS.toMillis(2));

      // Configurações específicas para MySQL
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      config.addDataSourceProperty("useServerPrepStmts", "true");
      config.addDataSourceProperty("useLocalSessionState", "true");
      config.addDataSourceProperty("rewriteBatchedStatements", "true");
      config.addDataSourceProperty("cacheResultSetMetadata", "true");
      config.addDataSourceProperty("cacheServerConfiguration", "true");
      config.addDataSourceProperty("elideSetAutoCommits", "true");
      config.addDataSourceProperty("maintainTimeStats", "false");

      dataSource = new HikariDataSource(config);
      plugin.getLogger().info(ShopConstants.LOG_DATABASE_CONNECT);
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE,
          String.format(ShopConstants.LOG_DATABASE_ERROR, e.getMessage()));
    }
  }

  public Connection getConnection() throws SQLException {
    if (!enabled || dataSource == null) {
      throw new SQLException("Banco de dados não está habilitado");
    }

    try {
      return dataSource.getConnection();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE,
          String.format(ShopConstants.LOG_DATABASE_ERROR, e.getMessage()));
      throw e;
    }
  }

  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean testConnection() {
    if (!enabled) {
      return false;
    }

    try (Connection conn = getConnection()) {
      return conn != null && !conn.isClosed();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE,
          String.format(ShopConstants.LOG_DATABASE_ERROR, "Teste de conexão falhou: " + e.getMessage()));
      return false;
    }
  }
}
