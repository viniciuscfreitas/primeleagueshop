package com.primeleague.shop.services;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.events.ShopPreTransactionEvent;
import com.primeleague.shop.events.ShopTransactionEvent;
import com.primeleague.shop.models.ShopCategory;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.storage.dao.TransactionDAO;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.sql.SQLException;

/**
 * Gerencia as operações da loja, incluindo a compra e venda de itens
 */
public class ShopManager {

  private final PrimeLeagueShopPlugin plugin;
  private final EconomyService economyService;
  private final List<ShopCategory> categories;
  private final Map<String, CachedShopItem> itemCache;
  private TransactionDAO transactionDAO;
  private final Map<String, ShopItem> itemsById;
  private final Map<String, List<ShopItem>> itemsByCategory;

  private static class CachedShopItem {
    private final ShopItem item;
    private final long timestamp;

    public CachedShopItem(ShopItem item) {
      this.item = item;
      this.timestamp = System.currentTimeMillis();
    }

    public ShopItem getItem() {
      return item;
    }

    public long getLastAccess() {
      return timestamp;
    }
  }

  /**
   * Cria um novo gerenciador de loja
   *
   * @param plugin         Instância do plugin
   * @param economyService Serviço de economia
   */
  public ShopManager(PrimeLeagueShopPlugin plugin, EconomyService economyService) {
    this.plugin = plugin;
    this.economyService = economyService;
    this.categories = new ArrayList<>();
    this.itemCache = new HashMap<>();
    this.itemsById = new HashMap<>();
    this.itemsByCategory = new HashMap<>();

    // Inicializa as categorias a partir do config loader
    reloadCategories();

    // Inicializa o DAO de transações se necessário
    if (plugin.getConfigLoader().shouldLogToDatabase()) {
      try {
        this.transactionDAO = new TransactionDAO(plugin);
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Erro ao inicializar o banco de dados", e);
      }
    }

    loadItems();
  }

  /**
   * Recarrega as categorias e itens da loja
   */
  public void reloadCategories() {
    categories.clear();
    itemCache.clear();

    // Carrega as categorias do arquivo de configuração
    List<ShopCategory> loadedCategories = plugin.getConfigLoader().loadShop();
    categories.addAll(loadedCategories);

    // Popula o cache de itens para pesquisa rápida
    for (ShopCategory category : categories) {
      for (ShopItem item : category.getItems()) {
        itemCache.put(item.getName().toLowerCase(), new CachedShopItem(item));
      }
    }

    plugin.getLogger().info("Carregadas " + categories.size() + " categorias e " +
        itemCache.size() + " itens da loja.");
  }

  private void loadItems() {
    ConfigurationSection categoriesSection = plugin.getConfigLoader().getShopConfig()
        .getConfigurationSection("categories");
    if (categoriesSection == null) {
      plugin.getLogger().warning("Nenhuma categoria encontrada na configuração da loja!");
      return;
    }

    for (String categoryId : categoriesSection.getKeys(false)) {
      ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
      if (categorySection == null)
        continue;

      String categoryName = categorySection.getString("name", categoryId);
      ConfigurationSection itemsSection = categorySection.getConfigurationSection("items");

      if (itemsSection == null) {
        plugin.getLogger().warning("Nenhum item encontrado na categoria: " + categoryName);
        continue;
      }

      for (String itemId : itemsSection.getKeys(false)) {
        ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
        if (itemSection == null)
          continue;

        String materialName = itemSection.getString("material");
        if (materialName == null) {
          plugin.getLogger()
              .warning("Material não especificado para o item: " + itemId + " na categoria: " + categoryName);
          continue;
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null) {
          plugin.getLogger().warning("Material inválido para o item: " + itemId + " na categoria: " + categoryName);
          continue;
        }

        byte data = (byte) itemSection.getInt("data", 0);
        String displayName = itemSection.getString("name", material.name());
        List<String> description = new ArrayList<>();
        double buyPrice = itemSection.getDouble("buy_price", -1);
        double sellPrice = itemSection.getDouble("sell_price", -1);
        String permission = itemSection.getString("permission", "");
        List<String> lore = itemSection.getStringList("lore");

        ShopItem item = new ShopItem(material, data, displayName, description, buyPrice, sellPrice, permission, lore,
            null);
        itemsById.put(itemId, item);

        List<ShopItem> categoryItems = itemsByCategory.computeIfAbsent(categoryName, k -> new ArrayList<>());
        categoryItems.add(item);
      }
    }

    plugin.getLogger().info("Carregados " + itemsById.size() + " itens em " + itemsByCategory.size() + " categorias.");
  }

  /**
   * Processa uma transação de compra
   *
   * @param player   Jogador
   * @param item     Item a comprar
   * @param quantity Quantidade
   * @return true se a compra foi bem-sucedida
   */
  public boolean buyItem(Player player, ShopItem item, int quantity) {
    // Verifica permissões
    if (!player.hasPermission(ShopConstants.PERM_BUY)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
      return false;
    }

    if (item.getPermission() != null && !item.getPermission().isEmpty() &&
        !player.hasPermission(item.getPermission())) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("item_no_permission",
              "&cVocê não tem permissão para comprar este item.")));
      return false;
    }

    // Calcula o preço total
    double price = item.calculatePrice(quantity, true);

    // Verifica se o jogador tem dinheiro suficiente
    if (!economyService.hasMoney(player, price)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader()
              .getMessage("not_enough_money", "&cVocê não tem dinheiro suficiente. Necessário: &e{price}{currency}")
              .replace("{price}", String.valueOf(price))
              .replace("{currency}", plugin.getConfigLoader().getCurrencySymbol())));
      return false;
    }

    // Cria a transação
    Transaction transaction = new Transaction(
        player.getName(),
        item,
        quantity,
        item.getBuyPrice(),
        Transaction.TransactionType.BUY);

    // Chama o evento de pré-transação
    ShopPreTransactionEvent preEvent = new ShopPreTransactionEvent(
        player,
        item,
        quantity,
        price,
        Transaction.TransactionType.BUY);

    plugin.getServer().getPluginManager().callEvent(preEvent);
    if (preEvent.isCancelled()) {
      if (!preEvent.getCancelReason().isEmpty()) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() + preEvent.getCancelReason()));
      }
      return false;
    }

    // Aplica possíveis mudanças do evento
    quantity = preEvent.getQuantity();
    price = preEvent.getTotalPrice();

    // Verifica se o jogador tem espaço no inventário
    if (!hasInventorySpace(player, item, quantity)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("inventory_full", "&cSeu inventário está cheio!")));
      return false;
    }

    // Realiza a transação
    boolean moneyWithdrawn = economyService.withdrawMoney(player, price);
    if (!moneyWithdrawn) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("economy_error", "&cOcorreu um erro com a economia. Tente novamente.")));
      return false;
    }

    // Entrega o item
    ItemStack itemStack = item.createActualItem(quantity);
    player.getInventory().addItem(itemStack);

    // Registra a transação
    transaction.markSuccessful();
    logTransaction(transaction);

    // Chama o evento de transação concluída
    ShopTransactionEvent event = new ShopTransactionEvent(player, transaction);
    plugin.getServer().getPluginManager().callEvent(event);

    // Envia mensagem de sucesso
    player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
        TextUtils.formatBuyMessage(
            plugin.getConfigLoader().getMessage("item_bought",
                "&aVocê comprou &e{quantity}x {item} &apor &e{price}{currency}!"),
            item.getName(),
            quantity,
            price,
            plugin.getConfigLoader().getCurrencySymbol())));

    return true;
  }

  /**
   * Processa uma transação de venda
   *
   * @param player   Jogador
   * @param item     Item a vender
   * @param quantity Quantidade
   * @return true se a venda foi bem-sucedida
   */
  public boolean sellItem(Player player, ShopItem item, int quantity) {
    // Verifica permissões
    if (!player.hasPermission(ShopConstants.PERM_SELL)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
      return false;
    }

    // Calcula o preço total
    double price = item.calculatePrice(quantity, false);

    // Cria a transação
    Transaction transaction = new Transaction(
        player.getName(),
        item,
        quantity,
        item.getSellPrice(),
        Transaction.TransactionType.SELL);

    // Chama o evento de pré-transação
    ShopPreTransactionEvent preEvent = new ShopPreTransactionEvent(
        player,
        item,
        quantity,
        price,
        Transaction.TransactionType.SELL);

    plugin.getServer().getPluginManager().callEvent(preEvent);
    if (preEvent.isCancelled()) {
      if (!preEvent.getCancelReason().isEmpty()) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() + preEvent.getCancelReason()));
      }
      return false;
    }

    // Aplica possíveis mudanças do evento
    quantity = preEvent.getQuantity();
    price = preEvent.getTotalPrice();

    // Verifica se o jogador tem os itens necessários
    if (!hasItem(player, item, quantity)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("not_enough_items", "&cVocê não tem itens suficientes para vender.")));
      return false;
    }

    // Remove os itens
    removeItems(player, item, quantity);

    // Adiciona o dinheiro
    boolean moneyDeposited = economyService.depositMoney(player, price);
    if (!moneyDeposited) {
      // Tenta devolver os itens em caso de falha
      ItemStack itemStack = item.createActualItem(quantity);
      player.getInventory().addItem(itemStack);

      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("economy_error", "&cOcorreu um erro com a economia. Tente novamente.")));
      return false;
    }

    // Registra a transação
    transaction.markSuccessful();
    logTransaction(transaction);

    // Chama o evento de transação concluída
    ShopTransactionEvent event = new ShopTransactionEvent(player, transaction);
    plugin.getServer().getPluginManager().callEvent(event);

    // Envia mensagem de sucesso
    player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
        TextUtils.formatBuyMessage(
            plugin.getConfigLoader().getMessage("item_sold",
                "&aVocê vendeu &e{quantity}x {item} &apor &e{price}{currency}!"),
            item.getName(),
            quantity,
            price,
            plugin.getConfigLoader().getCurrencySymbol())));

    return true;
  }

  /**
   * Registra uma transação no log e opcionalmente no banco de dados
   *
   * @param transaction Transação a registrar
   */
  private void logTransaction(Transaction transaction) {
    // Log no console se configurado
    if (plugin.getConfigLoader().shouldLogToConsole()) {
      String logMessage = String.format(ShopConstants.LOG_TRANSACTION,
          String.format("%s %s %dx %s por %.2f%s",
              transaction.getPlayerName(),
              transaction.getType() == Transaction.TransactionType.BUY ? "comprou" : "vendeu",
              transaction.getQuantity(),
              transaction.getItem().getName(),
              transaction.getTotalPrice(),
              plugin.getConfigLoader().getCurrencySymbol()));
      plugin.getLogger().info(logMessage);
    }

    // Log no banco de dados se configurado
    if (plugin.getConfigLoader().shouldLogToDatabase() && transactionDAO != null) {
      try {
        transactionDAO.saveTransaction(transaction);
      } catch (Exception e) {
        plugin.getLogger().log(Level.WARNING,
            String.format(ShopConstants.LOG_DATABASE_ERROR, "Erro ao salvar transação"), e);
      }
    }
  }

  /**
   * Verifica se o jogador tem espaço no inventário para receber itens
   *
   * @param player   Jogador
   * @param item     Item a verificar
   * @param quantity Quantidade
   * @return true se houver espaço
   */
  private boolean hasInventorySpace(Player player, ShopItem item, int quantity) {
    ItemStack testItem = item.createActualItem(quantity);
    return player.getInventory().firstEmpty() != -1 || player.getInventory().contains(testItem.getType());
  }

  /**
   * Verifica se o jogador possui a quantidade específica de um item
   *
   * @param player   Jogador
   * @param item     Item a verificar
   * @param quantity Quantidade necessária
   * @return true se o jogador tiver os itens
   */
  private boolean hasItem(Player player, ShopItem item, int quantity) {
    int count = 0;
    for (ItemStack stack : player.getInventory().getContents()) {
      if (stack != null && item.matches(stack)) {
        count += stack.getAmount();
        if (count >= quantity) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Remove uma quantidade específica de um item do inventário do jogador
   *
   * @param player   Jogador
   * @param item     Item a remover
   * @param quantity Quantidade a remover
   */
  private void removeItems(Player player, ShopItem item, int quantity) {
    int remaining = quantity;
    ItemStack[] contents = player.getInventory().getContents();

    for (int i = 0; i < contents.length && remaining > 0; i++) {
      ItemStack stack = contents[i];
      if (stack != null && item.matches(stack)) {
        int amount = stack.getAmount();
        if (amount <= remaining) {
          player.getInventory().setItem(i, null);
          remaining -= amount;
        } else {
          stack.setAmount(amount - remaining);
          player.getInventory().setItem(i, stack);
          remaining = 0;
        }
      }
    }

    player.updateInventory();
  }

  /**
   * Procura um item pelo nome
   *
   * @param itemName Nome do item
   * @return Item ou null se não encontrado
   */
  public ShopItem findItemByName(String itemName) {
    CachedShopItem cached = itemCache.get(itemName.toLowerCase());
    return cached != null ? cached.getItem() : null;
  }

  /**
   * Obtém todas as categorias da loja
   *
   * @return Lista de categorias
   */
  public List<ShopCategory> getCategories() {
    return categories;
  }

  /**
   * Desliga o gerenciador de loja
   * Fecha conexões com banco de dados, etc.
   */
  public void shutdown() {
    if (transactionDAO != null) {
      transactionDAO.closeConnection();
    }
    itemsById.clear();
    itemsByCategory.clear();
  }

  public ShopItem getItemById(String itemId) {
    return itemsById.get(itemId);
  }

  public List<ShopItem> getItemsByCategory(String category) {
    return itemsByCategory.getOrDefault(category, new ArrayList<>());
  }

  public List<String> getCategoryNames() {
    return new ArrayList<>(itemsByCategory.keySet());
  }

  /**
   * Limpa os dados de cache de um jogador específico
   *
   * @param playerName Nome do jogador
   */
  public void cleanupPlayerData(String playerName) {
    // Remove dados do jogador dos caches
    itemCache.remove(playerName);
  }

  /**
   * Executa limpeza geral dos caches
   */
  public void runCacheCleanup() {
    // Remove itens expirados do cache
    long now = System.currentTimeMillis();
    synchronized (itemCache) {
      itemCache.entrySet().removeIf(entry -> now - entry.getValue().getLastAccess() > 3600000); // 1 hora
    }
  }

  /**
   * Processa uma transação de compra
   *
   * @param player      Jogador
   * @param transaction Transação a ser processada
   * @return true se a compra foi bem-sucedida
   */
  public boolean processBuy(Player player, Transaction transaction) {
    // Verifica permissões
    if (!player.hasPermission(ShopConstants.PERM_BUY)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
      return false;
    }

    ShopItem item = transaction.getItem();
    if (item.getPermission() != null && !item.getPermission().isEmpty() &&
        !player.hasPermission(item.getPermission())) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("item_no_permission",
              "&cVocê não tem permissão para comprar este item.")));
      return false;
    }

    // Calcula o preço total
    double totalPrice = transaction.getTotalPrice();

    // Verifica se o jogador tem dinheiro suficiente
    if (!economyService.hasMoney(player, totalPrice)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader()
              .getMessage("not_enough_money", "&cVocê não tem dinheiro suficiente. Necessário: &e{price}{currency}")
              .replace("{price}", String.format("%.2f", totalPrice))
              .replace("{currency}", plugin.getConfigLoader().getCurrencySymbol())));
      return false;
    }

    // Processa o pagamento
    if (!economyService.withdrawMoney(player, totalPrice)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("transaction_failed", "&cErro ao processar a transação.")));
      return false;
    }

    // Dá o item para o jogador
    ItemStack itemStack = item.createActualItem(transaction.getQuantity());
    player.getInventory().addItem(itemStack);

    // Envia mensagem de sucesso
    String message = plugin.getConfigLoader().getMessage("buy_success",
        "&aVocê comprou &e{quantity}x {item} &apor &e{price}{currency}");
    player.sendMessage(TextUtils.formatBuyMessage(message,
        item.getName(),
        transaction.getQuantity(),
        totalPrice,
        plugin.getConfigLoader().getCurrencySymbol()));

    // Registra a transação no banco de dados se necessário
    if (plugin.getConfigLoader().shouldLogToDatabase() && transactionDAO != null) {
      try {
        transactionDAO.saveTransaction(transaction);
      } catch (SQLException e) {
        plugin.getLogger().log(Level.SEVERE, "Erro ao salvar transação no banco de dados", e);
      }
    }

    transaction.markSuccessful();
    return true;
  }

  /**
   * Processa uma transação de venda
   *
   * @param player      Jogador
   * @param transaction Transação a ser processada
   * @return true se a venda foi bem-sucedida
   */
  public boolean processSell(Player player, Transaction transaction) {
    // Verifica permissões
    if (!player.hasPermission(ShopConstants.PERM_SELL)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
      return false;
    }

    ShopItem item = transaction.getItem();
    int quantity = transaction.getQuantity();

    // Verifica se o jogador tem os itens
    if (!hasItem(player, item, quantity)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("not_enough_items",
              "&cVocê não tem itens suficientes para vender.")));
      return false;
    }

    // Calcula o valor total
    double totalPrice = transaction.getTotalPrice();

    // Remove os itens do inventário
    removeItems(player, item, quantity);

    // Dá o dinheiro para o jogador
    if (!economyService.depositMoney(player, totalPrice)) {
      // Se falhar ao depositar o dinheiro, tenta devolver os itens
      ItemStack itemStack = item.createActualItem(quantity);
      player.getInventory().addItem(itemStack);

      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("transaction_failed", "&cErro ao processar a transação.")));
      return false;
    }

    // Envia mensagem de sucesso
    String message = plugin.getConfigLoader().getMessage("sell_success",
        "&aVocê vendeu &e{quantity}x {item} &apor &e{price}{currency}");
    player.sendMessage(TextUtils.formatBuyMessage(message,
        item.getName(),
        quantity,
        totalPrice,
        plugin.getConfigLoader().getCurrencySymbol()));

    // Registra a transação no banco de dados se necessário
    if (plugin.getConfigLoader().shouldLogToDatabase() && transactionDAO != null) {
      try {
        transactionDAO.saveTransaction(transaction);
      } catch (SQLException e) {
        plugin.getLogger().log(Level.SEVERE, "Erro ao salvar transação no banco de dados", e);
      }
    }

    transaction.markSuccessful();
    return true;
  }
}
