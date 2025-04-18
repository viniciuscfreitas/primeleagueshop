package com.primeleague.shop;

import com.primeleague.shop.commands.ShopCommand;
import com.primeleague.shop.gui.CategoryGUI;
import com.primeleague.shop.gui.ConfirmationGUI;
import com.primeleague.shop.gui.ShopGUI;
import com.primeleague.shop.listeners.ShopInventoryListener;
import com.primeleague.shop.listeners.ChatListener;
import com.primeleague.shop.services.EconomyService;
import com.primeleague.shop.services.ShopManager;
import com.primeleague.shop.storage.ShopConfigLoader;
import com.primeleague.shop.services.DynamicPricingService;
import com.primeleague.shop.services.PlayerPreferencesManager;
import com.primeleague.shop.services.TransactionHistoryManager;
import com.primeleague.shop.ranking.RankingManager;
import com.primeleague.shop.services.FeedbackManager;
import com.primeleague.shop.database.DatabaseManager;
import com.primeleague.shop.utils.ShopConstants;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.primeleague.shop.combat.CombatManager;
import com.primeleague.shop.listeners.PlayerListener;
import com.primeleague.shop.listeners.CombatListener;
import com.primeleague.shop.storage.TransactionHistory;
import com.primeleague.shop.utils.LogManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.primeleague.shop.services.FeedbackService;
import com.primeleague.shop.services.FavoriteService;
import com.primeleague.shop.tutorial.ShopTutorial;
import com.primeleague.shop.services.ChatInputManager;
import com.primeleague.shop.services.CartManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class PrimeLeagueShopPlugin extends JavaPlugin {

  private static PrimeLeagueShopPlugin instance;
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
  private LogManager logManager;
  private Economy economy;

  // GUIs compartilhadas
  private ShopGUI shopGUI;
  private CategoryGUI categoryGUI;
  private ConfirmationGUI confirmationGUI;

  private FeedbackService feedbackService;
  private FavoriteService favoriteService;

  private ShopTutorial shopTutorial;

  private ChatInputManager chatInputManager;
  private CartManager cartManager;

  public static PrimeLeagueShopPlugin getInstance() {
    return instance;
  }

  @Override
  public void onEnable() {
    instance = this;

    // Inicializa gerenciadores
    this.configLoader = new ShopConfigLoader(this);
    this.configLoader.loadAll();

    // Setup economia
    if (!setupEconomy()) {
      getLogger().severe("Vault não encontrado! Desabilitando plugin...");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    // Inicializa serviços
    this.economyService = new EconomyService(this);
    this.shopManager = new ShopManager(this, economy);
    this.chatInputManager = new ChatInputManager();
    this.cartManager = new CartManager(this);
    this.transactionHistory = new TransactionHistory(this);
    this.pricingService = new DynamicPricingService(this);
    this.preferencesManager = new PlayerPreferencesManager(this);
    this.historyManager = new TransactionHistoryManager(this);
    this.feedbackManager = new FeedbackManager(this);
    this.logManager = new LogManager(this);

    // Carrega dados
    shopManager.reloadCategories();

    // Inicializa GUIs primeiro
    this.shopGUI = new ShopGUI(this);
    this.categoryGUI = new CategoryGUI(this);
    this.confirmationGUI = new ConfirmationGUI(this);

    // Registra eventos depois das GUIs
    getServer().getPluginManager().registerEvents(new ShopInventoryListener(this), this);
    getServer().getPluginManager().registerEvents(new ChatListener(this), this);

    // Registra comandos
    getCommand("shop").setExecutor(new ShopCommand(this));

    transactionHistory.initializeDatabase();

    // Inicializa o RankingManager
    rankingManager = new RankingManager(this);

    getLogger().info("Plugin habilitado com sucesso!");
  }

  @Override
  public void onDisable() {
    // Desliga os serviços
    if (shopManager != null) {
      shopManager.shutdown();
    }

    if (transactionHistory != null) {
      transactionHistory.close();
    }

    getLogger().info(ShopConstants.LOG_PLUGIN_DISABLED);
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

  public LogManager getLogManager() {
    return logManager;
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

  private boolean setupEconomy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
      return false;
    }
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      return false;
    }
    economy = rsp.getProvider();
    return economy != null;
  }

  public Economy getEconomy() {
    return economy;
  }

  public FeedbackService getFeedbackService() {
    return feedbackService;
  }

  public FavoriteService getFavoriteService() {
    return favoriteService;
  }

  public ShopTutorial getShopTutorial() {
    return shopTutorial;
  }

  public ChatInputManager getChatInputManager() {
    return chatInputManager;
  }

  public CartManager getCartManager() {
    return cartManager;
  }
}
