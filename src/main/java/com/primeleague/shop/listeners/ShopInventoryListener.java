package com.primeleague.shop.listeners;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.gui.CategoryGUI;
import com.primeleague.shop.gui.ConfirmationGUI;
import com.primeleague.shop.gui.ShopGUI;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

/**
 * Listener para eventos de inventário relacionados à loja
 */
public class ShopInventoryListener implements Listener {

  private final PrimeLeagueShopPlugin plugin;
  private final ShopGUI shopGUI;
  private final CategoryGUI categoryGUI;
  private final ConfirmationGUI confirmationGUI;

  /**
   * Cria um novo listener
   *
   * @param plugin Instância do plugin
   */
  public ShopInventoryListener(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.shopGUI = plugin.getShopGUI();
    this.categoryGUI = plugin.getCategoryGUI();
    this.confirmationGUI = plugin.getConfirmationGUI();
  }

  /**
   * Processa cliques no inventário
   *
   * @param event Evento de clique
   */
  @EventHandler(priority = EventPriority.HIGH)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getWhoClicked();
    String title = event.getView().getTitle();

    // Verifica se é um inventário da loja
    if (!isShopInventory(title)) {
      return;
    }

    // Cancela QUALQUER interação quando a loja está aberta
    event.setCancelled(true);

    // Se o clique foi no inventário do jogador, ignora
    Inventory clicked = event.getInventory();
    if (clicked == null || clicked.equals(player.getInventory())) {
      return;
    }

    // Debug log
    plugin.getLogger().info(String.format("Clique processado: Slot=%d, LeftClick=%b, ShiftClick=%b",
        event.getSlot(), event.isLeftClick(), event.isShiftClick()));

    // Processa o clique baseado no tipo de GUI
    if (title.equals(
        TextUtils.colorize(plugin.getConfigLoader().getMessage("gui.main_shop_title", "&8Loja Prime League")))) {
      shopGUI.handleClick(player, event.getSlot());
    } else if (title
        .contains(TextUtils.colorize(plugin.getConfigLoader().getMessage("gui.category_title", "&8Categoria:")))) {
      categoryGUI.handleClick(
          player,
          event.getSlot(),
          event.isLeftClick(),
          event.isShiftClick());
    } else if (title
        .equals(TextUtils.colorize(plugin.getConfigLoader().getMessage("gui.confirmation_title", "&8Confirmar")))) {
      confirmationGUI.handleClick(
          player,
          event.getSlot(),
          event.isLeftClick());
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }

    String title = event.getView().getTitle();
    if (isShopInventory(title)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInventoryMoveItem(InventoryMoveItemEvent event) {
    Inventory source = event.getSource();
    Inventory destination = event.getDestination();

    if (isShopInventory(source.getTitle()) || isShopInventory(destination.getTitle())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInventoryPickupItem(InventoryPickupItemEvent event) {
    Inventory inventory = event.getInventory();
    if (isShopInventory(inventory.getTitle())) {
      event.setCancelled(true);
    }
  }

  /**
   * Registra quando um inventário é fechado
   *
   * @param event Evento de fechamento de inventário
   */
  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getPlayer();
    String title = event.getView().getTitle();

    // Limpa dados do jogador quando ele fecha o inventário
    if (isShopInventory(title)) {
      categoryGUI.removePlayerData(player);
      confirmationGUI.removePlayerData(player);
    }
  }

  /**
   * Limpa os dados quando o jogador desconecta
   *
   * @param event Evento de saída do jogador
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    categoryGUI.removePlayerData(player);
    confirmationGUI.removePlayerData(player);
  }

  /**
   * Verifica se um inventário é da loja baseado no título
   *
   * @param title Título do inventário
   * @return true se for um inventário da loja
   */
  private boolean isShopInventory(String title) {
    String mainShopTitle = TextUtils
        .colorize(plugin.getConfigLoader().getMessage("gui.main_shop_title", "&8Loja Prime League"));
    String categoryTitle = TextUtils
        .colorize(plugin.getConfigLoader().getMessage("gui.category_title", "&8Categoria:"));
    String confirmationTitle = TextUtils
        .colorize(plugin.getConfigLoader().getMessage("gui.confirmation_title", "&8Confirmar"));

    return title.equals(mainShopTitle) ||
        title.contains(categoryTitle) ||
        title.equals(confirmationTitle);
  }
}
