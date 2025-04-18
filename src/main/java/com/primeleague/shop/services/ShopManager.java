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
import com.primeleague.shop.utils.Cleanable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import net.milkbowl.vault.economy.Economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.sql.SQLException;

/**
 * Gerencia as operações da loja, incluindo a compra e venda de itens
 */
public class ShopManager implements Cleanable {

  private final PrimeLeagueShopPlugin plugin;
  private final Economy economy;
  private final List<ShopCategory> categories;
  private final Map<String, CachedShopItem> itemCache;
  private TransactionDAO transactionDAO;
  private final Map<String, ShopItem> itemsById;
  private final Map<String, List<ShopItem>> itemsByCategory;
  private final Map<String, RateLimit> rateLimits = new HashMap<>();
  private static final long RATE_LIMIT_WINDOW = 60000; // 1 minuto
  private static final int MAX_TRANSACTIONS = 30; // máximo de transações por minuto
  private final Map<String, Long> transactionCooldowns = new HashMap<>();
  private static final long COOLDOWN_DURATION = 2000; // 2 segundos entre transações

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

  private static class RateLimit {
    private int count;
    private long windowStart;

    public RateLimit() {
      this.count = 0;
      this.windowStart = System.currentTimeMillis();
    }

    public boolean tryAcquire() {
      long now = System.currentTimeMillis();
      if (now - windowStart > RATE_LIMIT_WINDOW) {
        count = 0;
        windowStart = now;
      }
      if (count >= MAX_TRANSACTIONS) {
        return false;
      }
      count++;
      return true;
    }
  }

  /**
   * Cria um novo gerenciador de loja
   *
   * @param plugin         Instância do plugin
   * @param economy        Serviço de economia
   */
  public ShopManager(PrimeLeagueShopPlugin plugin, Economy economy) {
    this.plugin = plugin;
    this.economy = economy;
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

  private synchronized boolean checkRateLimit(Player player) {
    RateLimit limit = rateLimits.computeIfAbsent(player.getName(), k -> new RateLimit());
    return limit.tryAcquire();
  }

  private boolean checkCooldown(Player player) {
    String playerName = player.getName();
    Long lastTransaction = transactionCooldowns.get(playerName);
    long now = System.currentTimeMillis();

    if (lastTransaction != null && now - lastTransaction < COOLDOWN_DURATION) {
      long remaining = (lastTransaction + COOLDOWN_DURATION - now) / 1000;
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("cooldown",
        "&cAguarde {seconds} segundos para realizar outra transação.")
        .replace("{seconds}", String.valueOf(remaining + 1))));
      return false;
    }

    transactionCooldowns.put(playerName, now);
    return true;
  }

  /**
   * Processa uma compra
   * @param player Jogador
   * @param item Item
   * @param quantity Quantidade
   * @return true se sucesso
   */
  public boolean processPurchase(Player player, ShopItem item, int quantity) {
    double totalCost = item.getBuyPrice() * quantity;

    // Verifica se tem dinheiro suficiente
    if (!economy.has(player.getName(), totalCost)) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("insufficient_money",
            "&cVocê não tem dinheiro suficiente! Necessário: {currency}{price}")
            .replace("{currency}", plugin.getConfigLoader().getCurrencySymbol())
            .replace("{price}", String.format("%.2f", totalCost))));
        return false;
    }

    // Verifica espaço no inventário
    List<ItemStack> items = item.createActualItems(quantity);
    int slotsNeeded = items.size();
    int slotsAvailable = 0;

    for (ItemStack content : player.getInventory().getContents()) {
        if (content == null || content.getType() == Material.AIR) {
            slotsAvailable++;
        }
    }

    if (slotsAvailable < slotsNeeded) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("inventory_full",
            "&cSeu inventário está cheio! Necessário: {slots} slots livres")
            .replace("{slots}", String.valueOf(slotsNeeded))));
        return false;
    }

    // Processa a compra
    economy.withdrawPlayer(player.getName(), totalCost);

    // Entrega os itens
    for (ItemStack itemStack : items) {
        player.getInventory().addItem(itemStack);
    }

    return true;
  }

  /**
   * Processa uma venda
   * @param player Jogador
   * @param item Item
   * @param quantity Quantidade
   * @return true se sucesso
   */
  public boolean processSale(Player player, ShopItem item, int quantity) {
    // Verifica se o jogador tem os itens necessários
    int itemCount = 0;
    for (ItemStack invItem : player.getInventory().getContents()) {
        if (invItem != null && item.matches(invItem)) {
            itemCount += invItem.getAmount();
        }
    }

    if (itemCount < quantity) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("insufficient_items",
            "&cVocê não tem itens suficientes! Necessário: {quantity}x")
            .replace("{quantity}", String.valueOf(quantity))));
        return false;
    }

    // Calcula o valor total
    double totalValue = item.getSellPrice() * quantity;

    // Remove os itens do inventário
    int remaining = quantity;
    ItemStack[] contents = player.getInventory().getContents();

    for (int i = 0; i < contents.length && remaining > 0; i++) {
        ItemStack invItem = contents[i];
        if (invItem != null && item.matches(invItem)) {
            int toRemove = Math.min(remaining, invItem.getAmount());
            remaining -= toRemove;

            if (toRemove == invItem.getAmount()) {
                contents[i] = null;
            } else {
                invItem.setAmount(invItem.getAmount() - toRemove);
            }
        }
    }

    player.getInventory().setContents(contents);

    // Adiciona o dinheiro
    economy.depositPlayer(player.getName(), totalValue);

    return true;
  }

  /**
   * Registra uma transação no log e opcionalmente no banco de dados
   *
   * @param transaction Transação a registrar
   */
  private void logTransaction(Transaction transaction) {
    if (plugin.isDatabaseEnabled() && transactionDAO != null) {
        try {
            transactionDAO.saveTransaction(transaction);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao salvar transação no banco", e);
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
  private boolean hasInventorySpace(Player player, ItemStack item) {
    return player.getInventory().firstEmpty() != -1 || player.getInventory().contains(item.getType());
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
    ItemStack checkItem = item.createActualItems(1).get(0);
    int remaining = quantity;
    ItemStack[] contents = player.getInventory().getContents();

    for (int i = 0; i < contents.length && remaining > 0; i++) {
        ItemStack invItem = contents[i];
        if (invItem != null && invItem.isSimilar(checkItem)) {
            int amount = invItem.getAmount();
            if (amount <= remaining) {
                contents[i] = null;
                remaining -= amount;
            } else {
                invItem.setAmount(amount - remaining);
                remaining = 0;
            }
        }
    }

    player.getInventory().setContents(contents);
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
    if (!economy.has(player.getName(), totalPrice)) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
            plugin.getConfigLoader()
                .getMessage("not_enough_money", "&cVocê não tem dinheiro suficiente. Necessário: &e{price}{currency}")
                .replace("{price}", String.format("%.2f", totalPrice))
                .replace("{currency}", plugin.getConfigLoader().getCurrencySymbol())));
        return false;
    }

    // Verifica espaço no inventário
    List<ItemStack> items = item.createActualItems(transaction.getQuantity());
    int slotsNeeded = items.size();
    int slotsAvailable = 0;

    for (ItemStack content : player.getInventory().getContents()) {
        if (content == null || content.getType() == Material.AIR) {
            slotsAvailable++;
        }
    }

    if (slotsAvailable < slotsNeeded) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("inventory_full",
            "&cSeu inventário está cheio! Necessário: {slots} slots livres")
            .replace("{slots}", String.valueOf(slotsNeeded))));
        return false;
    }

    // Processa o pagamento
    economy.withdrawPlayer(player.getName(), totalPrice);

    // Dá os itens para o jogador
    for (ItemStack itemStack : items) {
        player.getInventory().addItem(itemStack);
    }

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
    economy.depositPlayer(player.getName(), totalPrice);

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

  private boolean validatePurchase(Player player, ShopItem item, int quantity) {
    double totalPrice = item.getBuyPrice() * quantity;

    // Verifica se o jogador tem dinheiro suficiente
    if (!economy.has(player.getName(), totalPrice)) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
            plugin.getConfigLoader()
                .getMessage("not_enough_money", "&cVocê não tem dinheiro suficiente. Necessário: &e{price}{currency}")
                .replace("{price}", String.format("%.2f", totalPrice))
                .replace("{currency}", plugin.getConfigLoader().getCurrencySymbol())));
        return false;
    }

    // Processa o pagamento
    economy.withdrawPlayer(player.getName(), totalPrice);

    return true;
  }

  private boolean validateSale(Player player, ShopItem item, int quantity) {
    double totalPrice = item.getSellPrice() * quantity;

    // Dá o dinheiro para o jogador
    economy.depositPlayer(player.getName(), totalPrice);

    return true;
  }

  private boolean hasItems(Player player, ShopItem item, int quantity) {
    ItemStack checkItem = item.createActualItems(1).get(0);
    int found = 0;

    for (ItemStack invItem : player.getInventory().getContents()) {
        if (invItem != null && invItem.isSimilar(checkItem)) {
            found += invItem.getAmount();
            if (found >= quantity) {
                return true;
            }
        }
    }

    return false;
  }

  private void cleanupCooldowns() {
    long now = System.currentTimeMillis();
    transactionCooldowns.entrySet().removeIf(entry ->
      now - entry.getValue() > COOLDOWN_DURATION * 2
    );
  }

  @Override
  public void cleanup() {
    cleanupCooldowns();
    cleanupCache();
  }

  private void cleanupCache() {
    long now = System.currentTimeMillis();
    itemCache.entrySet().removeIf(entry ->
      now - entry.getValue().getLastAccess() > 3600000 // 1 hora
    );
  }

  public ShopItem getItem(Material material, byte data) {
    String itemId = material.name() + ":" + data;
    CachedShopItem cached = itemCache.get(itemId);

    if (cached != null) {
      return cached.getItem();
    }

    for (ShopCategory category : categories) {
      for (ShopItem item : category.getItems()) {
        if (item.getMaterial() == material && item.getData() == data) {
          itemCache.put(itemId, new CachedShopItem(item));
          return item;
        }
      }
    }

    return null;
  }

  /**
   * Verifica se o jogador tem espaço suficiente no inventário
   */
  private boolean hasInventorySpace(Player player, ShopItem item, int quantity) {
    List<ItemStack> items = item.createActualItems(quantity);
    int slotsNeeded = items.size();
    int slotsAvailable = 0;

    for (ItemStack content : player.getInventory().getContents()) {
      if (content == null || content.getType() == Material.AIR) {
        slotsAvailable++;
      }
    }

    return slotsAvailable >= slotsNeeded;
  }

  /**
   * Recupera um item da loja pelo material e data value
   * @param material Nome do material
   * @param data Data value do item
   * @return ShopItem se encontrado, null caso contrário
   */
  public ShopItem getItemByMaterialAndData(String material, byte data) {
    plugin.getLogger().info("[Debug] Procurando item por material: " + material + " e data: " + data);

    // Primeiro tenta encontrar pelo nome do material
    for (ShopCategory category : categories) {
        plugin.getLogger().info("[Debug] Verificando categoria: " + category.getName());

        for (ShopItem item : category.getItems()) {
            plugin.getLogger().info("[Debug] Comparando com item: " + item.getName() +
                " (Material: " + item.getMaterial().name() + ")");

            // Compara o material de várias formas
            if (item.getMaterial().name().equalsIgnoreCase(material) ||
                item.getMaterial().name().replace("_", "").equalsIgnoreCase(material.replace("_", "")) ||
                item.getMaterial().name().replace(" ", "_").equalsIgnoreCase(material.replace(" ", "_"))) {

                // Se a data corresponder ou for 0
                if (item.getData() == data || data == 0) {
                    plugin.getLogger().info("[Debug] Item encontrado pelo nome do material!");
                    return item;
                }
            }
        }
    }

    // Se não encontrou, tenta pelo ID (para compatibilidade)
    try {
        int materialId = Integer.parseInt(material);
        Material matchedMaterial = null;

        // Mapeamento de IDs comuns
        switch (materialId) {
            case 276:
                matchedMaterial = Material.DIAMOND_SWORD;
                break;
            case 264:
                matchedMaterial = Material.DIAMOND;
                break;
            case 266:
                matchedMaterial = Material.GOLD_INGOT;
                break;
            case 265:
                matchedMaterial = Material.IRON_INGOT;
                break;
            default:
                matchedMaterial = Material.getMaterial(materialId);
        }

        if (matchedMaterial != null) {
            plugin.getLogger().info("[Debug] Material encontrado por ID: " + materialId + " -> " + matchedMaterial.name());

            for (ShopCategory category : categories) {
                for (ShopItem item : category.getItems()) {
                    if (item.getMaterial() == matchedMaterial && (item.getData() == data || data == 0)) {
                        plugin.getLogger().info("[Debug] Item encontrado por ID do material!");
                        return item;
                    }
                }
            }
        }
    } catch (NumberFormatException e) {
        // Ignora se não for um número
    }

    plugin.getLogger().info("[Debug] Nenhum item encontrado");
    return null;
  }
}
