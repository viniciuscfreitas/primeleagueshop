package com.primeleague.shop.storage;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopCategory;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.ItemUtils;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Responsável por carregar configurações de arquivos YAML
 */
public class ShopConfigLoader {

  private final PrimeLeagueShopPlugin plugin;
  private FileConfiguration shopConfig;
  private FileConfiguration messagesConfig;
  private final Map<String, String> messages;
  private final Map<String, String> guiMessages;

  /**
   * Cria um novo loader de configuração
   *
   * @param plugin Instância do plugin
   */
  public ShopConfigLoader(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.messages = new HashMap<>();
    this.guiMessages = new HashMap<>();
  }

  /**
   * Carrega todas as configurações
   *
   * @return true se carregou com sucesso
   */
  public boolean loadAll() {
    try {
      loadMessages();
      loadShop();
      return true;
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE,
          String.format(ShopConstants.LOG_CONFIG_ERROR, "Erro ao carregar configurações"), e);
      return false;
    }
  }

  /**
   * Carrega as categorias e itens da loja
   *
   * @return Lista de categorias
   */
  public List<ShopCategory> loadShop() {
    File shopFile = new File(plugin.getDataFolder(), "shop.yml");
    shopConfig = YamlConfiguration.loadConfiguration(shopFile);

    List<ShopCategory> categories = new ArrayList<>();

    ConfigurationSection categoriesSection = shopConfig.getConfigurationSection("categories");
    if (categoriesSection == null) {
      plugin.getLogger().log(Level.WARNING,
          String.format(ShopConstants.LOG_CONFIG_ERROR, "Nenhuma categoria encontrada no arquivo shop.yml"));
      return categories;
    }

    for (String categoryKey : categoriesSection.getKeys(false)) {
      ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryKey);
      if (categorySection == null)
        continue;

      String name = categorySection.getString("name", "Categoria");
      String materialName = categorySection.getString("icon", "STONE");
      Material material = ItemUtils.getMaterialByName(materialName);
      byte data = (byte) categorySection.getInt("data", 0);
      int slot = categorySection.getInt("slot", 0);
      String permission = categorySection.getString("permission", "");

      ShopCategory category = new ShopCategory(name, material, data, slot, permission);
      categories.add(category);

      ConfigurationSection itemsSection = categorySection.getConfigurationSection("items");
      if (itemsSection == null) {
        plugin.getLogger().log(Level.WARNING,
            String.format(ShopConstants.LOG_CONFIG_ERROR, "Nenhum item encontrado na categoria " + name));
        continue;
      }

      for (String itemKey : itemsSection.getKeys(false)) {
        ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
        if (itemSection == null)
          continue;

        String itemName = itemSection.getString("name", "Item");
        String itemMaterialName = itemSection.getString("material", "STONE");
        Material itemMaterial = ItemUtils.getMaterialByName(itemMaterialName);
        byte itemData = (byte) itemSection.getInt("data", 0);
        double buyPrice = itemSection.getDouble("buy_price", 0);
        double sellPrice = itemSection.getDouble("sell_price", 0);
        String itemPermission = itemSection.getString("permission", "");

        List<String> lore = itemSection.getStringList("lore");
        if (lore.isEmpty()) {
          lore = new ArrayList<>();
          lore.add("&7Preço de compra: &f{buy_price}{currency}");
          lore.add("&7Preço de venda: &f{sell_price}{currency}");
        }

        for (int i = 0; i < lore.size(); i++) {
          lore.set(i, TextUtils.colorize(lore.get(i)));
        }

        ShopItem item = new ShopItem(itemMaterial, itemData, itemName, new ArrayList<>(), buyPrice, sellPrice,
            itemPermission, lore, category);
        category.addItem(item);
      }
    }

    return categories;
  }

  /**
   * Carrega as mensagens
   */
  private void loadMessages() {
    File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

    messages.clear();
    guiMessages.clear();

    // Carrega mensagens principais
    ConfigurationSection messagesSection = messagesConfig.getConfigurationSection("messages");
    if (messagesSection != null) {
      for (String key : messagesSection.getKeys(false)) {
        String message = messagesSection.getString(key, "");
        messages.put(key, TextUtils.colorize(message));
      }
    }

    // Carrega mensagens da GUI
    ConfigurationSection guiSection = messagesConfig.getConfigurationSection("gui");
    if (guiSection != null) {
      for (String key : guiSection.getKeys(false)) {
        String message = guiSection.getString(key, "");
        guiMessages.put(key, TextUtils.colorize(message));
      }
    }
  }

  /**
   * Obtém uma mensagem
   *
   * @param key          Chave da mensagem
   * @param defaultValue Valor padrão
   * @return A mensagem ou o valor padrão
   */
  public String getMessage(String key, String defaultValue) {
    return messages.getOrDefault(key, defaultValue);
  }

  /**
   * Obtém uma mensagem da GUI
   *
   * @param key          Chave da mensagem
   * @param defaultValue Valor padrão
   * @return A mensagem ou o valor padrão
   */
  public String getGuiMessage(String key, String defaultValue) {
    return guiMessages.getOrDefault(key, defaultValue);
  }

  /**
   * Obtém o prefixo das mensagens
   *
   * @return Prefixo
   */
  public String getPrefix() {
    return getMessage("prefix", "&8[&aShop&8] ");
  }

  /**
   * Obtém o símbolo da moeda
   *
   * @return Símbolo da moeda
   */
  public String getCurrencySymbol() {
    return plugin.getConfig().getString("settings.economy.currency-symbol", "$");
  }

  /**
   * Verifica se deve usar o Vault
   *
   * @return true se deve usar o Vault
   */
  public boolean useVault() {
    return plugin.getConfig().getBoolean("settings.economy.use-vault", true);
  }

  /**
   * Obtém o título da GUI principal
   *
   * @return Título da GUI principal
   */
  public String getMainGuiTitle() {
    return getGuiMessage("main_shop_title", "&8Loja");
  }

  /**
   * Obtém o título da GUI de categoria
   *
   * @param categoryName Nome da categoria
   * @return Título da GUI de categoria
   */
  public String getCategoryGuiTitle(String categoryName) {
    String title = getGuiMessage("category_title", "&8Categoria: &a{category}")
        .replace("{category}", categoryName);
    return TextUtils.colorize(title);
  }

  /**
   * Obtém o número de linhas para a GUI
   *
   * @return Número de linhas
   */
  public int getGuiRows() {
    return plugin.getConfig().getInt("settings.gui.rows", 6);
  }

  /**
   * Verifica se deve preencher slots vazios
   *
   * @return true se deve preencher
   */
  public boolean shouldFillEmptySlots() {
    return plugin.getConfig().getBoolean("settings.gui.fill-empty-slots", true);
  }

  /**
   * Obtém o material para preenchimento
   *
   * @return Material para preenchimento
   */
  public Material getFillMaterial() {
    String materialName = plugin.getConfig().getString("settings.gui.fill-material", "STAINED_GLASS_PANE");
    return ItemUtils.getMaterialByName(materialName);
  }

  /**
   * Obtém o data value para preenchimento
   *
   * @return Data value
   */
  public byte getFillData() {
    return (byte) plugin.getConfig().getInt("settings.gui.fill-data", 15);
  }

  /**
   * Obtém a quantidade máxima de compra
   *
   * @return Quantidade máxima
   */
  public int getMaxBuyQuantity() {
    return plugin.getConfig().getInt("settings.transaction.max-buy-quantity", 64);
  }

  /**
   * Obtém a quantidade máxima de venda
   *
   * @return Quantidade máxima
   */
  public int getMaxSellQuantity() {
    return plugin.getConfig().getInt("settings.transaction.max-sell-quantity", 64);
  }

  /**
   * Obtém o preço mínimo para mostrar confirmação
   *
   * @return Preço mínimo
   */
  public double getConfirmAbovePrice() {
    return plugin.getConfig().getDouble("settings.transaction.confirm-above-price", 1000);
  }

  /**
   * Verifica se deve logar transações no banco de dados
   *
   * @return true se deve logar
   */
  public boolean shouldLogToDatabase() {
    return plugin.getConfig().getBoolean("settings.transaction.log-to-database", false);
  }

  /**
   * Verifica se deve logar transações no console
   *
   * @return true se deve logar
   */
  public boolean shouldLogToConsole() {
    return plugin.getConfig().getBoolean("settings.transaction.log-to-console", true);
  }

  public ConfigurationSection getShopItems() {
    return shopConfig.getConfigurationSection("items");
  }

  /**
   * Obtém a configuração da loja
   *
   * @return Configuração da loja
   */
  public FileConfiguration getShopConfig() {
    return shopConfig;
  }
}
