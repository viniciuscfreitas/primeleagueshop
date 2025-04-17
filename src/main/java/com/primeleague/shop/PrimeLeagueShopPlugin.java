package com.primeleague.shop;

import com.primeleague.shop.commands.ShopCommand;
import com.primeleague.shop.gui.CategoryGUI;
import com.primeleague.shop.gui.ConfirmationGUI;
import com.primeleague.shop.gui.ShopGUI;
import com.primeleague.shop.listeners.ShopInventoryListener;
import com.primeleague.shop.services.EconomyService;
import com.primeleague.shop.services.ShopManager;
import com.primeleague.shop.storage.ShopConfigLoader;
import com.primeleague.shop.services.DynamicPricingService;
import com.primeleague.shop.services.PlayerPreferencesManager;
import com.primeleague.shop.services.TransactionHistoryManager;
import com.primeleague.shop.services.RankingManager;
import com.primeleague.shop.services.FeedbackManager;
import com.primeleague.shop.database.DatabaseManager;
import com.primeleague.shop.utils.ShopConstants;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.primeleague.shop.combat.CombatManager;
import com.primeleague.shop.listeners.PlayerListener;
import com.primeleague.shop.listeners.CombatListener;
import com.primeleague.shop.storage.TransactionHistory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class PrimeLeagueShopPlugin extends JavaPlugin {

  private ShopManager shopManager;
  private EconomyService economyService;
  private ShopConfigLoader configLoader;
  private DynamicPricingService pricingService;
  private PlayerPreferencesManager preferencesManager;
  private TransactionHistoryManager historyManager;
  private RankingManager rankingManager;
  private FeedbackManager feedbackManager;
  private DatabaseManager databaseManager;
  private CombatManager combatManager;
  private TransactionHistory transactionHistory;

  // GUIs compartilhadas
  private ShopGUI shopGUI;
  private CategoryGUI categoryGUI;
  private ConfirmationGUI confirmationGUI;

  @Override
  public void onEnable() {
    try {
      // Cria o diretório do plugin se não existir
      if (!getDataFolder().exists()) {
        getDataFolder().mkdirs();
      }

      // Inicializando os arquivos de configuração
      saveDefaultConfig();

      // Lista de arquivos necessários
      String[] configFiles = { "shop.yml", "messages.yml" };

      for (String fileName : configFiles) {
        if (!new java.io.File(getDataFolder(), fileName).exists()) {
          saveResource(fileName, false);
          getLogger().info("Arquivo " + fileName + " criado com sucesso!");
        }
      }

      // Carregando configurações
      configLoader = new ShopConfigLoader(this);
      if (!configLoader.loadAll()) {
        getLogger().severe("Erro ao carregar configurações! O plugin será desabilitado.");
        getServer().getPluginManager().disablePlugin(this);
        return;
      }

      // Verifica se os arquivos foram carregados corretamente
      if (!validateConfigs()) {
        getLogger().severe("Arquivos de configuração inválidos! O plugin será desabilitado.");
        getServer().getPluginManager().disablePlugin(this);
        return;
      }

      // Iniciando banco de dados se necessário
      if (getConfig().getBoolean("settings.transaction.log-to-database", false)) {
        databaseManager = new DatabaseManager(this);
      }

      // Iniciando serviços
      economyService = new EconomyService(this);
      pricingService = new DynamicPricingService(this);
      preferencesManager = new PlayerPreferencesManager(this);
      historyManager = new TransactionHistoryManager(this);
      rankingManager = new RankingManager(this);
      feedbackManager = new FeedbackManager(this);
      combatManager = new CombatManager(this);
      transactionHistory = new TransactionHistory(this);

      // Iniciando shop manager após outros serviços
      shopManager = new ShopManager(this, economyService);

      // Iniciando GUIs
      shopGUI = new ShopGUI(this);
      categoryGUI = new CategoryGUI(this);
      confirmationGUI = new ConfirmationGUI(this);

      // Registrando comandos
      getCommand("shop").setExecutor(new ShopCommand(this));

      // Registrando eventos
      PluginManager pm = getServer().getPluginManager();
      pm.registerEvents(new ShopInventoryListener(this), this);
      pm.registerEvents(new PlayerListener(this), this);
      pm.registerEvents(new CombatListener(this), this);

      // Inicia tarefas de limpeza
      startCleanupTasks();

      getLogger().info("PrimeLeagueShop ativado com sucesso!");

    } catch (Exception e) {
      getLogger().severe("Erro ao inicializar o plugin: " + e.getMessage());
      e.printStackTrace();
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  private void startCleanupTasks() {
    // Executa limpeza a cada 5 minutos
    getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
      @Override
      public void run() {
        combatManager.cleanup();
        preferencesManager.runCacheCleanup();
        shopManager.runCacheCleanup();
        transactionHistory.cleanup();
      }
    }, 6000L, 6000L); // 5 minutos = 6000 ticks
  }

  @Override
  public void onDisable() {
    // Salvando dados pendentes
    if (shopManager != null) {
      shopManager.shutdown();
    }
    if (historyManager != null) {
      historyManager.shutdown();
    }
    if (rankingManager != null) {
      rankingManager.shutdown();
    }
    if (preferencesManager != null) {
      preferencesManager.saveAll();
    }
    if (databaseManager != null) {
      databaseManager.close();
    }
    if (transactionHistory != null) {
      transactionHistory.close();
    }

    getLogger().info("PrimeLeagueShop desativado com sucesso!");
  }

  /**
   * Obtém uma conexão do pool de conexões
   */
  public Connection getConnection() throws SQLException {
    if (databaseManager == null || !databaseManager.isEnabled()) {
      throw new SQLException("Banco de dados não está habilitado");
    }
    return databaseManager.getConnection();
  }

  /**
   * Verifica se o banco de dados está habilitado
   */
  public boolean isDatabaseEnabled() {
    return databaseManager != null && databaseManager.isEnabled();
  }

  /**
   * Recarrega todas as configurações do plugin
   *
   * @return true se o reload foi bem sucedido
   */
  public boolean reload() {
    reloadConfig();
    return configLoader.loadAll();
  }

  // Getters para os serviços
  public ShopManager getShopManager() {
    return shopManager;
  }

  public EconomyService getEconomyService() {
    return economyService;
  }

  public ShopConfigLoader getConfigLoader() {
    return configLoader;
  }

  public DynamicPricingService getPricingService() {
    return pricingService;
  }

  public PlayerPreferencesManager getPreferencesManager() {
    return preferencesManager;
  }

  public TransactionHistoryManager getHistoryManager() {
    return historyManager;
  }

  public RankingManager getRankingManager() {
    return rankingManager;
  }

  public FeedbackManager getFeedbackManager() {
    return feedbackManager;
  }

  public CombatManager getCombatManager() {
    return combatManager;
  }

  public TransactionHistory getTransactionHistory() {
    return transactionHistory;
  }

  // Getters para as GUIs
  public ShopGUI getShopGUI() {
    return shopGUI;
  }

  public CategoryGUI getCategoryGUI() {
    return categoryGUI;
  }

  public ConfirmationGUI getConfirmationGUI() {
    return confirmationGUI;
  }

  /**
   * Valida se os arquivos de configuração foram carregados corretamente
   */
  private boolean validateConfigs() {
    try {
      // Verifica se os arquivos existem e podem ser lidos
      java.io.File configFile = new java.io.File(getDataFolder(), "config.yml");
      java.io.File shopFile = new java.io.File(getDataFolder(), "shop.yml");
      java.io.File messagesFile = new java.io.File(getDataFolder(), "messages.yml");

      if (!configFile.exists() || !shopFile.exists() || !messagesFile.exists()) {
        getLogger().severe("Arquivos de configuração não encontrados!");
        return false;
      }

      // Verifica se o shop.yml tem categorias
      if (getConfigLoader().getShopConfig().getConfigurationSection("categories") == null) {
        getLogger().severe("Arquivo shop.yml não contém categorias!");
        return false;
      }

      return true;
    } catch (Exception e) {
      getLogger().severe("Erro ao validar configurações: " + e.getMessage());
      return false;
    }
  }
}
