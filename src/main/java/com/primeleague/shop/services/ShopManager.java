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
 * Gerencia as operações da loja, incluindo compra, venda e gerenciamento de itens
 */
public class ShopManager implements Cleanable {

    private final PrimeLeagueShopPlugin plugin;
    private final Economy economy;
    private final List<ShopCategory> categories;
    private final Map<String, CachedShopItem> itemCache;
    private final Map<String, ShopItem> itemsById;
    private final Map<String, List<ShopItem>> itemsByCategory;
    private TransactionDAO transactionDAO;

    /**
     * Classe interna para cache de itens
     */
    private static class CachedShopItem {
        private final ShopItem item;
        private final long lastAccess;

        public CachedShopItem(ShopItem item) {
            this.item = item;
            this.lastAccess = System.currentTimeMillis();
        }

        public ShopItem getItem() {
            return item;
        }

        public long getLastAccess() {
            return lastAccess;
        }
    }

    public ShopManager(PrimeLeagueShopPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.categories = new ArrayList<>();
        this.itemCache = new HashMap<>();
        this.itemsById = new HashMap<>();
        this.itemsByCategory = new HashMap<>();

        reloadCategories();

        if (plugin.getConfigLoader().shouldLogToDatabase()) {
            try {
                this.transactionDAO = new TransactionDAO(plugin);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao inicializar o banco de dados", e);
            }
        }
    }

    /**
     * Recarrega as categorias e itens da loja
     */
    public void reloadCategories() {
        categories.clear();
        itemCache.clear();
        itemsById.clear();
        itemsByCategory.clear();

        List<ShopCategory> loadedCategories = plugin.getConfigLoader().loadShop();
        categories.addAll(loadedCategories);

        for (ShopCategory category : categories) {
            List<ShopItem> categoryItems = new ArrayList<>();
            for (ShopItem item : category.getItems()) {
                String itemId = item.getName().toLowerCase();
                itemCache.put(itemId, new CachedShopItem(item));
                itemsById.put(itemId, item);
                categoryItems.add(item);
            }
            itemsByCategory.put(category.getName(), categoryItems);
        }

        plugin.getLogger().info(String.format("Carregadas %d categorias e %d itens da loja",
            categories.size(), itemCache.size()));
    }

    /**
     * Processa uma transação de compra
     */
    public boolean processPurchase(Player player, ShopItem item, int quantity) {
        if (!validatePurchase(player, item, quantity)) {
            return false;
        }

        double totalPrice = item.calculatePrice(quantity, true);

        // Chama evento de pré-transação
        ShopPreTransactionEvent preEvent = new ShopPreTransactionEvent(
            player.getName(),
            item.getName(),
            quantity,
            totalPrice,
            true
        );
        plugin.getServer().getPluginManager().callEvent(preEvent);

        if (preEvent.isCancelled()) {
            return false;
        }

        // Processa a compra
        economy.withdrawPlayer(player.getName(), totalPrice);
        List<ItemStack> items = item.createActualItems(quantity);
        for (ItemStack stack : items) {
            player.getInventory().addItem(stack);
        }

        // Registra a transação
        Transaction transaction = new Transaction(
            player.getName(),
            item.getName(),
            quantity,
            totalPrice,
            true
        );
        logTransaction(transaction);

        // Chama evento pós-transação
        ShopTransactionEvent postEvent = new ShopTransactionEvent(
            player.getName(),
            item.getName(),
            quantity,
            totalPrice,
            true
        );
        plugin.getServer().getPluginManager().callEvent(postEvent);

        return true;
    }

    /**
     * Processa uma transação de venda
     */
    public boolean processSale(Player player, ShopItem item, int quantity) {
        if (!validateSale(player, item, quantity)) {
            return false;
        }

        double totalPrice = item.calculatePrice(quantity, false);

        // Chama evento de pré-transação
        ShopPreTransactionEvent preEvent = new ShopPreTransactionEvent(
            player.getName(),
            item.getName(),
            quantity,
            totalPrice,
            false
        );
        plugin.getServer().getPluginManager().callEvent(preEvent);

        if (preEvent.isCancelled()) {
            return false;
        }

        // Processa a venda
        economy.depositPlayer(player.getName(), totalPrice);
        removeItems(player, item, quantity);

        // Registra a transação
        Transaction transaction = new Transaction(
            player.getName(),
            item.getName(),
            quantity,
            totalPrice,
            false
        );
        logTransaction(transaction);

        // Chama evento pós-transação
        ShopTransactionEvent postEvent = new ShopTransactionEvent(
            player.getName(),
            item.getName(),
            quantity,
            totalPrice,
            false
        );
        plugin.getServer().getPluginManager().callEvent(postEvent);

        return true;
    }

    /**
     * Valida uma compra
     */
    private boolean validatePurchase(Player player, ShopItem item, int quantity) {
        if (quantity <= 0 || quantity > plugin.getConfigLoader().getMaxBuyQuantity()) {
            player.sendMessage(TextUtils.colorize(ShopConstants.MSG_INVALID_AMOUNT
                .replace("{max}", String.valueOf(plugin.getConfigLoader().getMaxBuyQuantity()))));
            return false;
        }

        double totalPrice = item.calculatePrice(quantity, true);
        if (!economy.has(player.getName(), totalPrice)) {
            player.sendMessage(TextUtils.colorize(ShopConstants.MSG_NOT_ENOUGH_MONEY
                .replace("{price}", economy.format(totalPrice))));
            return false;
        }

        return true;
    }

    /**
     * Valida uma venda
     */
    private boolean validateSale(Player player, ShopItem item, int quantity) {
        if (quantity <= 0 || quantity > plugin.getConfigLoader().getMaxSellQuantity()) {
            player.sendMessage(TextUtils.colorize(ShopConstants.MSG_INVALID_AMOUNT
                .replace("{max}", String.valueOf(plugin.getConfigLoader().getMaxSellQuantity()))));
            return false;
        }

        if (!hasItems(player, item, quantity)) {
            player.sendMessage(TextUtils.colorize(ShopConstants.MSG_NOT_ENOUGH_ITEMS));
            return false;
        }

        return true;
    }

    /**
     * Remove itens do inventário do jogador
     */
    private void removeItems(Player player, ShopItem item, int quantity) {
        int remaining = quantity;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (item.matches(stack)) {
                int amount = Math.min(remaining, stack.getAmount());
                if (amount == stack.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - amount);
                }
                remaining -= amount;
            }
        }
    }

    /**
     * Verifica se o jogador tem os itens necessários
     */
    private boolean hasItems(Player player, ShopItem item, int quantity) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (item.matches(stack)) {
                count += stack.getAmount();
                if (count >= quantity) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Registra uma transação no banco de dados
     */
    private void logTransaction(Transaction transaction) {
        if (transactionDAO != null) {
            try {
                transactionDAO.saveTransaction(transaction);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                    String.format(ShopConstants.LOG_DATABASE_ERROR, e.getMessage()), e);
            }
        }
    }

    /**
     * Procura um item pelo nome
     */
    public ShopItem findItemByName(String itemName) {
        CachedShopItem cached = itemCache.get(itemName.toLowerCase());
        return cached != null ? cached.getItem() : null;
    }

    /**
     * Obtém um item pelo ID
     */
    public ShopItem getItemById(String itemId) {
        return itemsById.get(itemId.toLowerCase());
    }

    /**
     * Obtém itens de uma categoria
     */
    public List<ShopItem> getItemsByCategory(String category) {
        return itemsByCategory.getOrDefault(category, new ArrayList<>());
    }

    /**
     * Obtém nomes das categorias
     */
    public List<String> getCategoryNames() {
        return new ArrayList<>(itemsByCategory.keySet());
    }

    /**
     * Obtém todas as categorias
     */
    public List<ShopCategory> getCategories() {
        return categories;
    }

    /**
     * Limpa dados de cache de um jogador
     */
    public void cleanupPlayerData(String playerName) {
        itemCache.remove(playerName.toLowerCase());
    }

    @Override
    public void cleanup() {
        long now = System.currentTimeMillis();
        synchronized (itemCache) {
            itemCache.entrySet().removeIf(entry ->
                now - entry.getValue().getLastAccess() > 3600000); // 1 hora
        }
    }

    /**
     * Desliga o gerenciador
     */
    public void shutdown() {
        if (transactionDAO != null) {
            transactionDAO.closeConnection();
        }
        itemsById.clear();
        itemsByCategory.clear();
        itemCache.clear();
        categories.clear();
    }

    /**
     * Executa a limpeza do cache
     */
    public void runCacheCleanup() {
        cleanup();
    }

    /**
     * Obtém um item pelo material e data value
     * @param material Nome do material
     * @param data Data value
     * @return Item encontrado ou null se não existir
     */
    public ShopItem getItemByMaterialAndData(String material, byte data) {
        for (ShopCategory category : categories) {
            for (ShopItem item : category.getItems()) {
                if (item.getMaterial().name().equals(material) && item.getData() == data) {
                    return item;
                }
            }
        }
        return null;
    }
}
