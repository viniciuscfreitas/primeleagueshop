package com.primeleague.shop.gui;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.sql.Timestamp;

import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.ItemBuilder;

public class ConfirmationGUI {
  private final PrimeLeagueShopPlugin plugin;
  private final Map<UUID, ConfirmationData> playerData;

  private static class ConfirmationData {
    private final ShopItem item;
    private int quantity;
    private final boolean isBuying;

    public ConfirmationData(ShopItem item, int quantity, boolean isBuying) {
      this.item = item;
      this.quantity = quantity;
      this.isBuying = isBuying;
    }
  }

  public ConfirmationGUI(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.playerData = new HashMap<>();
  }

  public void openBuyConfirmation(Player player, ShopItem item, boolean isBuying) {
    Inventory inv = Bukkit.createInventory(null, 36, TextUtils.colorize("&8Confirmar " + (isBuying ? "Compra" : "Venda")));
    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();

    // Item central
    ItemStack displayItem = item.createDisplayItem(currencySymbol);
    ItemMeta meta = displayItem.getItemMeta();
    List<String> lore = meta.getLore();
    lore.add("");
    lore.add(TextUtils.colorize("&aQuantidade: &f1"));
    double totalPrice = isBuying ? item.getBuyPrice() : item.getSellPrice();
    lore.add(TextUtils.colorize("&aPreço total: &f" + currencySymbol + String.format("%.2f", totalPrice)));
    meta.setLore(lore);
    displayItem.setItemMeta(meta);
    inv.setItem(ShopConstants.INFO_SLOT, displayItem);

    // Botão de confirmar
    ItemStack confirmButton = createConfirmButton();
    inv.setItem(ShopConstants.CONFIRM_BUTTON_SLOT, confirmButton);

    // Botão de cancelar
    ItemStack cancelButton = createCancelButton();
    inv.setItem(ShopConstants.CANCEL_BUTTON_SLOT, cancelButton);

    // Botão de aumentar quantidade
    ItemStack increaseButton = createIncreaseButton();
    inv.setItem(ShopConstants.INCREASE_SLOT, increaseButton);

    // Botão de diminuir quantidade
    ItemStack decreaseButton = createDecreaseButton();
    inv.setItem(ShopConstants.DECREASE_SLOT, decreaseButton);

    // Preenche slots vazios
    ItemStack fillerItem = createQuantityItem(1);
    for (int i = 0; i < inv.getSize(); i++) {
      if (inv.getItem(i) == null) {
        inv.setItem(i, fillerItem);
      }
    }

    // Salva dados do jogador
    playerData.put(player.getUniqueId(), new ConfirmationData(item, 1, isBuying));
    player.openInventory(inv);
  }

  public void handleClick(Player player, int slot, boolean isShiftClick) {
    // Cancela qualquer tentativa de mover itens
    if (slot < 0 || slot >= 36) {
        return;
    }

    ConfirmationData data = playerData.get(player.getUniqueId());
    if (data == null) {
        player.closeInventory();
        return;
    }

    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();

    switch (slot) {
        case ShopConstants.CONFIRM_BUTTON_SLOT:
            if (data.isBuying) {
                if (plugin.getShopManager().processPurchase(player, data.item, data.quantity)) {
                    // Registra a transação no histórico
                    Transaction transaction = new Transaction(
                        player.getName(),
                        data.item,
                        data.quantity,
                        data.item.getBuyPrice(),
                        Transaction.TransactionType.BUY,
                        new Timestamp(System.currentTimeMillis())
                    );
                    transaction.markSuccessful();
                    plugin.getTransactionHistory().addTransaction(transaction);

                    // Atualiza o ranking
                    plugin.getRankingManager().updateStats(
                        player.getName(),
                        data.item.getBuyPrice() * data.quantity,
                        true
                    );

                    player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("purchase_success",
                        "&aVocê comprou &e{quantity}x {item} &apor &e{currency}{price}")
                        .replace("{quantity}", String.valueOf(data.quantity))
                        .replace("{item}", data.item.getName())
                        .replace("{currency}", currencySymbol)
                        .replace("{price}", String.format("%.2f", data.item.getBuyPrice() * data.quantity))));
                }
            } else {
                if (plugin.getShopManager().processSale(player, data.item, data.quantity)) {
                    // Registra a transação no histórico
                    Transaction transaction = new Transaction(
                        player.getName(),
                        data.item,
                        data.quantity,
                        data.item.getSellPrice(),
                        Transaction.TransactionType.SELL,
                        new Timestamp(System.currentTimeMillis())
                    );
                    transaction.markSuccessful();
                    plugin.getTransactionHistory().addTransaction(transaction);

                    // Atualiza o ranking
                    plugin.getRankingManager().updateStats(
                        player.getName(),
                        data.item.getSellPrice() * data.quantity,
                        false
                    );

                    player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("sale_success",
                        "&aVocê vendeu &e{quantity}x {item} &apor &e{currency}{price}")
                        .replace("{quantity}", String.valueOf(data.quantity))
                        .replace("{item}", data.item.getName())
                        .replace("{currency}", currencySymbol)
                        .replace("{price}", String.format("%.2f", data.item.getSellPrice() * data.quantity))));
                }
            }
            player.closeInventory();
            playerData.remove(player.getUniqueId());
            break;

        case ShopConstants.CANCEL_BUTTON_SLOT:
            player.closeInventory();
            playerData.remove(player.getUniqueId());
            player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("transaction_cancelled",
                "&cTransação cancelada.")));
            break;

        case ShopConstants.DECREASE_SLOT:
            if (data.quantity > 1) {
                int decrease = isShiftClick ? 10 : 1;
                data.quantity = Math.max(1, data.quantity - decrease);
                updateQuantity(player, data);
            }
            break;

        case ShopConstants.INCREASE_SLOT:
            int maxQuantity = data.isBuying ?
                plugin.getConfigLoader().getMaxBuyQuantity() :
                plugin.getConfigLoader().getMaxSellQuantity();
            int increase = isShiftClick ? 10 : 1;
            data.quantity = Math.min(maxQuantity, data.quantity + increase);
            updateQuantity(player, data);
            break;

        default:
            // Cancela qualquer outro clique para evitar remoção de itens
            break;
    }
  }

  private void updateQuantity(Player player, ConfirmationData data) {
    Inventory inv = player.getOpenInventory().getTopInventory();
    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();

    // Atualiza item central usando createDisplayItem
    ItemStack displayItem = data.item.createDisplayItem(currencySymbol);
    ItemMeta meta = displayItem.getItemMeta();
    List<String> lore = meta.getLore();

    // Adiciona informações da transação
    lore.add("");
    lore.add(TextUtils.colorize("&aQuantidade: &f" + data.quantity));
    double totalPrice = data.isBuying ? data.item.getBuyPrice() * data.quantity : data.item.getSellPrice() * data.quantity;
    lore.add(TextUtils.colorize("&aPreço total: &f" + currencySymbol + String.format("%.2f", totalPrice)));

    meta.setLore(lore);
    displayItem.setItemMeta(meta);
    displayItem.setAmount(data.quantity);
    inv.setItem(ShopConstants.QUANTITY_SLOT, displayItem);

    // Atualiza botão de confirmar
    ItemStack confirmButton = inv.getItem(ShopConstants.CONFIRM_BUTTON_SLOT);
    ItemMeta confirmMeta = confirmButton.getItemMeta();
    List<String> confirmLore = new ArrayList<>();
    confirmLore.add(TextUtils.colorize("&7Clique para confirmar"));
    confirmLore.add(TextUtils.colorize("&7a " + (data.isBuying ? "compra" : "venda") + " de &f" + data.quantity + "x"));
    confirmLore.add(TextUtils.colorize("&7" + data.item.getName()));
    confirmLore.add(TextUtils.colorize("&7por &f" + currencySymbol + String.format("%.2f", totalPrice)));
    confirmMeta.setLore(confirmLore);
    confirmButton.setItemMeta(confirmMeta);
  }

  public void cleanup() {
    playerData.clear();
  }

  /**
   * Remove os dados de um jogador
   * @param player Jogador para remover os dados
   */
  public void removePlayerData(Player player) {
    playerData.remove(player.getUniqueId());
  }

  private ItemStack createConfirmButton() {
    ItemStack button = new ItemStack(Material.valueOf(ShopConstants.MATERIAL_CONFIRM));
    button.setDurability(ShopConstants.DATA_CONFIRM);
    ItemMeta meta = button.getItemMeta();
    meta.setDisplayName(ChatColor.GREEN + "Confirmar");
    button.setItemMeta(meta);
    return button;
  }

  private ItemStack createCancelButton() {
    ItemStack button = new ItemStack(Material.valueOf(ShopConstants.MATERIAL_CANCEL));
    button.setDurability(ShopConstants.DATA_CANCEL);
    ItemMeta meta = button.getItemMeta();
    meta.setDisplayName(ChatColor.RED + "Cancelar");
    button.setItemMeta(meta);
    return button;
  }

  private ItemStack createIncreaseButton() {
    ItemStack button = new ItemStack(Material.valueOf(ShopConstants.MATERIAL_INCREASE));
    button.setDurability(ShopConstants.DATA_INCREASE);
    ItemMeta meta = button.getItemMeta();
    meta.setDisplayName(ChatColor.GREEN + "Aumentar Quantidade");
    button.setItemMeta(meta);
    return button;
  }

  private ItemStack createDecreaseButton() {
    ItemStack button = new ItemStack(Material.valueOf(ShopConstants.MATERIAL_DECREASE));
    button.setDurability(ShopConstants.DATA_DECREASE);
    ItemMeta meta = button.getItemMeta();
    meta.setDisplayName(ChatColor.RED + "Diminuir Quantidade");
    button.setItemMeta(meta);
    return button;
  }

  private ItemStack createQuantityItem(int quantity) {
    ItemStack item = new ItemStack(Material.valueOf(ShopConstants.MATERIAL_QUANTITY));
    item.setDurability(ShopConstants.DATA_FILL);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(" ");
    item.setItemMeta(meta);
    return item;
  }
}
