package com.primeleague.shop.managers;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopCategory;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.milkbowl.vault.economy.Economy;

import java.util.List;
import java.util.Map;

public class ShopManager {
    private final PrimeLeagueShopPlugin plugin;
    private final Economy economy;
    private final Map<String, ShopCategory> categories;

    public ShopManager(PrimeLeagueShopPlugin plugin, Economy economy, Map<String, ShopCategory> categories) {
        this.plugin = plugin;
        this.economy = economy;
        this.categories = categories;
        plugin.getLogger().info("[Debug] ShopManager inicializado com " + categories.size() + " categorias");
        for (Map.Entry<String, ShopCategory> entry : categories.entrySet()) {
            plugin.getLogger().info("[Debug] Categoria: " + entry.getKey() + " com " + entry.getValue().getItems().size() + " itens");
            for (ShopItem item : entry.getValue().getItems()) {
                plugin.getLogger().info("[Debug] - Item: " + item.getName() + " (Material: " + item.getMaterial().name() + ")");
            }
        }
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

    public ShopItem getItemByMaterialAndData(String material, byte data) {
        plugin.getLogger().info("[Debug] Procurando item por material: " + material + " e data: " + data);

        // Primeiro tenta encontrar pelo nome do material
        for (Map.Entry<String, ShopCategory> entry : this.categories.entrySet()) {
            ShopCategory category = entry.getValue();
            plugin.getLogger().info("[Debug] Verificando categoria: " + category.getName());

            for (ShopItem item : category.getItems()) {
                plugin.getLogger().info("[Debug] Comparando com item: " + item.getName() +
                    " (Material: " + item.getMaterial().name() + ")");

                if (item.getMaterial().name().equals(material)) {
                    plugin.getLogger().info("[Debug] Item encontrado pelo nome do material!");
                    return item;
                }
            }
        }

        // Se não encontrou, tenta pelo ID (para compatibilidade)
        if (material.equals("276") || material.equals("DIAMOND_SWORD")) {
            plugin.getLogger().info("[Debug] Tentando encontrar espada de diamante por compatibilidade");

            for (Map.Entry<String, ShopCategory> entry : this.categories.entrySet()) {
                ShopCategory category = entry.getValue();
                for (ShopItem item : category.getItems()) {
                    if (item.getMaterial() == Material.DIAMOND_SWORD) {
                        plugin.getLogger().info("[Debug] Espada de diamante encontrada!");
                        return item;
                    }
                }
            }
        }

        plugin.getLogger().info("[Debug] Nenhum item encontrado");
        return null;
    }

    public ShopItem findItemByName(String name) {
        plugin.getLogger().info("[Debug] Procurando item por nome: " + name);

        // Primeiro tenta encontrar pelo nome de exibição
        for (Map.Entry<String, ShopCategory> entry : this.categories.entrySet()) {
            ShopCategory category = entry.getValue();
            plugin.getLogger().info("[Debug] Verificando categoria: " + category.getName());

            for (ShopItem item : category.getItems()) {
                plugin.getLogger().info("[Debug] Comparando com item: " + item.getName() +
                    " (Material: " + item.getMaterial().name() + ")");

                if (item.getName().equalsIgnoreCase(name) ||
                    item.getMaterial().name().equalsIgnoreCase(name)) {
                    plugin.getLogger().info("[Debug] Item encontrado!");
                    return item;
                }
            }
        }

        plugin.getLogger().info("[Debug] Nenhum item encontrado");
        return null;
    }
}
