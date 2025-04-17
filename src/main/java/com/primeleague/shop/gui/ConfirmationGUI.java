package com.primeleague.shop.gui;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.utils.ItemUtils;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cria e gerencia a GUI de confirmação de compra/venda
 */
public class ConfirmationGUI {

  private final PrimeLeagueShopPlugin plugin;
  private final Map<UUID, ConfirmationData> playerData;
  private static final int ROWS = 3;
  private static final int CONFIRM_SLOT = 11;
  private static final int CANCEL_SLOT = 15;
  private static final int INFO_SLOT = 13;

  /**
   * Cria uma nova GUI de confirmação
   *
   * @param plugin Instância do plugin
   */
  public ConfirmationGUI(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.playerData = new HashMap<>();
  }

  /**
   * Abre a GUI de confirmação para um jogador
   *
   * @param player   Jogador
   * @param item     Item selecionado
   * @param quantity Quantidade inicial
   * @param isBuying Se é uma compra (true) ou venda (false)
   */
  public void openConfirmationGUI(Player player, ShopItem item, int quantity, boolean isBuying) {
    String title = plugin.getConfigLoader().getMessage("gui.confirmation_title", "&8Confirmar");
    title = TextUtils.colorize(title);

    Inventory inventory = Bukkit.createInventory(null, ROWS * 9, title);
    playerData.put(player.getUniqueId(), new ConfirmationData(item, quantity, isBuying, title));

    // Item de informação
    ItemStack infoItem = item.createDisplayItem(plugin.getConfigLoader().getCurrencySymbol());
    ItemUtils.updateLore(infoItem, getInfoLore(item, quantity, isBuying));
    inventory.setItem(INFO_SLOT, infoItem);

    // Botão confirmar
    ItemStack confirmItem = ItemUtils.createItem(
        Material.EMERALD_BLOCK,
        "&aConfirmar",
        getConfirmLore(item, quantity, isBuying));
    inventory.setItem(CONFIRM_SLOT, confirmItem);

    // Botão cancelar
    ItemStack cancelItem = ItemUtils.createItem(
        Material.REDSTONE_BLOCK,
        "&cCancelar",
        (List<String>) null);
    inventory.setItem(CANCEL_SLOT, cancelItem);

    player.openInventory(inventory);
  }

  /**
   * Processa um clique na GUI de confirmação
   *
   * @param player      Jogador
   * @param slot        Slot clicado
   * @param isLeftClick true se o clique foi feito com o botão esquerdo
   */
  public void handleClick(Player player, int slot, boolean isLeftClick) {
    ConfirmationData data = playerData.get(player.getUniqueId());
    if (data == null)
      return;

    if (slot == CONFIRM_SLOT) {
      processTransaction(player, data);
    } else if (slot == CANCEL_SLOT) {
      player.closeInventory();
    }
  }

  private void processTransaction(Player player, ConfirmationData data) {
    Transaction transaction = new Transaction(
        player.getName(),
        data.item,
        data.quantity,
        data.isBuying ? data.item.getBuyPrice() : data.item.getSellPrice(),
        data.isBuying ? Transaction.TransactionType.BUY : Transaction.TransactionType.SELL);

    if (data.isBuying) {
      plugin.getShopManager().processBuy(player, transaction);
    } else {
      plugin.getShopManager().processSell(player, transaction);
    }

    player.closeInventory();
  }

  private String[] getInfoLore(ShopItem item, int quantity, boolean isBuying) {
    double price = isBuying ? item.getBuyPrice() : item.getSellPrice();
    double totalPrice = price * quantity;
    String action = isBuying ? "Comprar" : "Vender";
    return new String[] {
        "§7Quantidade: §f" + quantity,
        "§7Preço unitário: §f$" + String.format("%.2f", price),
        "§7Total: §f$" + String.format("%.2f", totalPrice),
        "",
        "§eClique em confirmar para " + action.toLowerCase()
    };
  }

  private String[] getConfirmLore(ShopItem item, int quantity, boolean isBuying) {
    double price = isBuying ? item.getBuyPrice() : item.getSellPrice();
    double totalPrice = price * quantity;
    String action = isBuying ? "Comprar" : "Vender";
    return new String[] {
        "§7" + action + " §f" + quantity + "x " + item.getName(),
        "§7por §f$" + String.format("%.2f", totalPrice)
    };
  }

  public void removePlayerData(Player player) {
    playerData.remove(player.getUniqueId());
  }

  /**
   * Classe para armazenar dados de confirmação
   */
  private static class ConfirmationData {
    private final ShopItem item;
    private int quantity;
    private final boolean isBuying;
    private final String inventoryTitle;

    public ConfirmationData(ShopItem item, int quantity, boolean isBuying, String title) {
      this.item = item;
      this.quantity = quantity;
      this.isBuying = isBuying;
      this.inventoryTitle = title;
    }
  }
}
