package com.primeleague.shop.gui;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class FavoritesGUI {
  private final PrimeLeagueShopPlugin plugin;
  private final int rows;
  private final String title;

  public FavoritesGUI(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.rows = plugin.getConfig().getInt("gui.favorites.rows", 6);
    this.title = plugin.getConfigLoader().getMessage("gui.favorites.title", "Seus Favoritos");
  }

  public void openFavoritesGUI(Player player) {
    Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
    List<String> favorites = plugin.getPreferencesManager().getFavorites(player.getName());

    int slot = 0;
    for (String itemId : favorites) {
      ShopItem shopItem = plugin.getShopManager().getItemById(itemId);
      if (shopItem != null) {
        ItemStack displayItem = shopItem.createDisplayItem("§7Compra: §e$");
        inventory.setItem(slot++, displayItem);
      }
    }

    player.openInventory(inventory);
  }
}
