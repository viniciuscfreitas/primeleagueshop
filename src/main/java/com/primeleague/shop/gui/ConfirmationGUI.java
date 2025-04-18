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
    // Cria um inventário 5x9 (45 slots) para melhor organização
    Inventory inv = Bukkit.createInventory(null, 45, TextUtils.colorize("&8Confirmar " + (isBuying ? "Compra" : "Venda")));
    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();

    // Borda decorativa
    ItemStack borderItem = new ItemStack(Material.THIN_GLASS);
    ItemMeta borderMeta = borderItem.getItemMeta();
    borderMeta.setDisplayName(" ");
    borderItem.setItemMeta(borderMeta);

    // Preenche a borda
    for (int i = 0; i < 9; i++) {
      inv.setItem(i, borderItem); // Linha superior
      inv.setItem(36 + i, borderItem); // Linha inferior
    }
    for (int i = 0; i < 5; i++) {
      inv.setItem(i * 9, borderItem); // Coluna esquerda
      inv.setItem(i * 9 + 8, borderItem); // Coluna direita
    }

    // Item central com informações detalhadas
    ItemStack displayItem = item.createDisplayItem(currencySymbol);
    displayItem.setAmount(1); // Começa com quantidade 1
    ItemMeta meta = displayItem.getItemMeta();
    List<String> lore = new ArrayList<>();
    lore.add("");
    lore.add(TextUtils.colorize("&fDetalhes da " + (isBuying ? "compra" : "venda") + ":"));
    lore.add(TextUtils.colorize("&7➥ Quantidade: &f1"));
    double pricePerUnit = isBuying ? item.getBuyPrice() : item.getSellPrice();
    lore.add(TextUtils.colorize("&7➥ Preço unitário: &f" + currencySymbol + String.format("%.2f", pricePerUnit)));
    lore.add(TextUtils.colorize("&7➥ Total: &f" + currencySymbol + String.format("%.2f", pricePerUnit)));
    lore.add("");
    if (isBuying) {
      double playerBalance = plugin.getEconomyService().getBalance(player);
      lore.add(TextUtils.colorize("&7Seu saldo: &f" + currencySymbol + String.format("%.2f", playerBalance)));
      lore.add(TextUtils.colorize("&7Saldo após compra: &f" + currencySymbol + String.format("%.2f", playerBalance - pricePerUnit)));
    }
    meta.setLore(lore);
    displayItem.setItemMeta(meta);
    inv.setItem(22, displayItem); // Centro do inventário

    // Botões de quantidade com visual melhorado
    ItemStack decreaseButton = new ItemStack(Material.WOOL, 1, (short) 14); // Vermelho
    ItemMeta decreaseMeta = decreaseButton.getItemMeta();
    decreaseMeta.setDisplayName(TextUtils.colorize("&c&l- Diminuir Quantidade"));
    List<String> decreaseLore = new ArrayList<>();
    decreaseLore.add(TextUtils.colorize("&7Clique para diminuir 1"));
    decreaseLore.add(TextUtils.colorize("&7Shift + Clique para diminuir 10"));
    decreaseMeta.setLore(decreaseLore);
    decreaseButton.setItemMeta(decreaseMeta);
    inv.setItem(21, decreaseButton);

    ItemStack increaseButton = new ItemStack(Material.WOOL, 1, (short) 5); // Verde
    ItemMeta increaseMeta = increaseButton.getItemMeta();
    increaseMeta.setDisplayName(TextUtils.colorize("&a&l+ Aumentar Quantidade"));
    List<String> increaseLore = new ArrayList<>();
    increaseLore.add(TextUtils.colorize("&7Clique para aumentar 1"));
    increaseLore.add(TextUtils.colorize("&7Shift + Clique para aumentar 10"));
    increaseMeta.setLore(increaseLore);
    increaseButton.setItemMeta(increaseMeta);
    inv.setItem(23, increaseButton);

    // Botões de confirmação e cancelamento mais visíveis
    ItemStack confirmButton = new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta confirmMeta = confirmButton.getItemMeta();
    confirmMeta.setDisplayName(TextUtils.colorize("&a&lCONFIRMAR"));
    List<String> confirmLore = new ArrayList<>();
    confirmLore.add(TextUtils.colorize("&7Clique para confirmar a " + (isBuying ? "compra" : "venda")));
    confirmLore.add(TextUtils.colorize("&7Total: &f" + currencySymbol + String.format("%.2f", pricePerUnit)));
    confirmMeta.setLore(confirmLore);
    confirmButton.setItemMeta(confirmMeta);
    inv.setItem(41, confirmButton);

    ItemStack cancelButton = new ItemStack(Material.REDSTONE_BLOCK);
    ItemMeta cancelMeta = cancelButton.getItemMeta();
    cancelMeta.setDisplayName(TextUtils.colorize("&c&lCANCELAR"));
    List<String> cancelLore = new ArrayList<>();
    cancelLore.add(TextUtils.colorize("&7Clique para cancelar a transação"));
    cancelMeta.setLore(cancelLore);
    cancelButton.setItemMeta(cancelMeta);
    inv.setItem(39, cancelButton);

    // Salva dados do jogador
    playerData.put(player.getUniqueId(), new ConfirmationData(item, 1, isBuying));
    player.openInventory(inv);
  }

  public void handleClick(Player player, int slot, boolean isShiftClick) {
    // Cancela qualquer tentativa de mover itens
    if (slot < 0 || slot >= 45) {
        return;
    }

    ConfirmationData data = playerData.get(player.getUniqueId());
    if (data == null) {
        player.closeInventory();
        return;
    }

    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();

    // Slots atualizados para o novo layout
    if (slot == 21) { // Botão de diminuir
        if (data.quantity > 1) {
            int decrease = isShiftClick ? 10 : 1;
            data.quantity = Math.max(1, data.quantity - decrease);
            updateQuantity(player, data);
        }
    } else if (slot == 23) { // Botão de aumentar
        int maxQuantity = data.isBuying ?
            plugin.getConfigLoader().getMaxBuyQuantity() :
            plugin.getConfigLoader().getMaxSellQuantity();
        int increase = isShiftClick ? 10 : 1;
        data.quantity = Math.min(maxQuantity, data.quantity + increase);
        updateQuantity(player, data);
    } else if (slot == 41) { // Botão de confirmar (movido para o slot do antigo botão de informações)
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
                plugin.getRankingManager().addTransaction(transaction);

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
                plugin.getRankingManager().addTransaction(transaction);

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
    } else if (slot == 39) { // Botão de cancelar
        player.closeInventory();
        playerData.remove(player.getUniqueId());
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getMessage("transaction_cancelled",
            "&cTransação cancelada.")));
    }
  }

  private void updateQuantity(Player player, ConfirmationData data) {
    Inventory inv = player.getOpenInventory().getTopInventory();
    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();

    // Atualiza item central
    ItemStack displayItem = data.item.createDisplayItem(currencySymbol);
    displayItem.setAmount(Math.min(64, data.quantity)); // Define a quantidade visual do item (máximo 64)
    ItemMeta meta = displayItem.getItemMeta();
    List<String> lore = new ArrayList<>();
    lore.add("");
    lore.add(TextUtils.colorize("&fDetalhes da " + (data.isBuying ? "compra" : "venda") + ":"));
    lore.add(TextUtils.colorize("&7➥ Quantidade: &f" + data.quantity));
    double pricePerUnit = data.isBuying ? data.item.getBuyPrice() : data.item.getSellPrice();
    double totalPrice = pricePerUnit * data.quantity;
    lore.add(TextUtils.colorize("&7➥ Preço unitário: &f" + currencySymbol + String.format("%.2f", pricePerUnit)));
    lore.add(TextUtils.colorize("&7➥ Total: &f" + currencySymbol + String.format("%.2f", totalPrice)));
    lore.add("");
    if (data.isBuying) {
      double playerBalance = plugin.getEconomyService().getBalance(player);
      lore.add(TextUtils.colorize("&7Seu saldo: &f" + currencySymbol + String.format("%.2f", playerBalance)));
      lore.add(TextUtils.colorize("&7Saldo após compra: &f" + currencySymbol + String.format("%.2f", playerBalance - totalPrice)));
    }
    meta.setLore(lore);
    displayItem.setItemMeta(meta);
    inv.setItem(22, displayItem);

    // Atualiza botão de confirmação
    ItemStack confirmButton = inv.getItem(41);
    ItemMeta confirmMeta = confirmButton.getItemMeta();
    List<String> confirmLore = new ArrayList<>();
    confirmLore.add(TextUtils.colorize("&7Clique para confirmar a " + (data.isBuying ? "compra" : "venda")));
    confirmLore.add(TextUtils.colorize("&7Total: &f" + currencySymbol + String.format("%.2f", totalPrice)));
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
