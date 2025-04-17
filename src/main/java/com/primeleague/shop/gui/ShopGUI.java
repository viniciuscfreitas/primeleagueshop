package com.primeleague.shop.gui;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopCategory;
import com.primeleague.shop.utils.ItemUtils;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cria e gerencia a GUI principal da loja
 */
public class ShopGUI {

  private final PrimeLeagueShopPlugin plugin;
  private final Map<Integer, ShopCategory> slotMap;

  // Constantes de log
  private static final String LOG_CLICK = "Menu principal: Clique no slot %d, categoria=%s";
  private static final String LOG_OPEN = "Menu principal: Abrindo para %s com %d categorias";

  /**
   * Cria uma nova GUI principal
   *
   * @param plugin Instância do plugin
   */
  public ShopGUI(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.slotMap = new HashMap<>();
  }

  /**
   * Abre o menu principal da loja para um jogador
   *
   * @param player Jogador
   */
  public void openMainMenu(Player player) {
    // Limpa o mapa de slots anterior
    slotMap.clear();

    String title = plugin.getConfigLoader().getMessage("gui.main_shop_title", "&8Loja Prime League");
    Inventory inv = Bukkit.createInventory(null, 54, TextUtils.colorize(title));

    List<ShopCategory> categories = plugin.getShopManager().getCategories();
    plugin.getLogger().info(String.format(LOG_OPEN, player.getName(), categories.size()));

    // Preenche slots vazios
    if (plugin.getConfigLoader().shouldFillEmptySlots()) {
      ItemStack filler = new ItemStack(
          plugin.getConfigLoader().getFillMaterial(),
          1,
          (short) plugin.getConfigLoader().getFillData());
      for (int i = 0; i < inv.getSize(); i++) {
        inv.setItem(i, filler);
      }
    }

    // Adiciona categorias
    int slot = 10;
    for (ShopCategory category : categories) {
      if (slot > 43)
        break; // Limite de slots

      ItemStack icon = category.getIcon();
      ItemMeta meta = icon.getItemMeta();
      meta.setDisplayName(TextUtils.colorize("&a" + category.getName()));

      List<String> lore = new ArrayList<>();
      lore.add(TextUtils.colorize("&7Clique para ver os itens"));
      lore.add(TextUtils.colorize("&7desta categoria"));
      meta.setLore(lore);

      icon.setItemMeta(meta);
      inv.setItem(slot, icon);

      // Mapeia o slot para a categoria
      slotMap.put(slot, category);
      plugin.getLogger().info("Mapeando categoria " + category.getName() + " para slot " + slot);

      // Próximo slot (pulando uma coluna)
      slot += (slot % 9 == 7) ? 3 : 1;
    }

    player.openInventory(inv);
    String message = plugin.getConfigLoader().getMessage("shop_opened", "&aVocê abriu a loja!");
    player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() + message));
  }

  /**
   * Processa um clique em um slot
   *
   * @param player Jogador
   * @param slot   Slot clicado
   * @return true se o slot continha uma categoria
   */
  public boolean handleClick(Player player, int slot) {
    plugin.getLogger().info(String.format(LOG_CLICK, slot,
        slotMap.containsKey(slot) ? slotMap.get(slot).getName() : "nenhuma"));

    // Verifica se o slot contém uma categoria
    if (slotMap.containsKey(slot)) {
      ShopCategory category = slotMap.get(slot);

      // Verifica permissão
      if (!category.getPermission().isEmpty() && !player.hasPermission(category.getPermission())) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
            plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
        return true;
      }

      // Abre a categoria usando a instância compartilhada
      plugin.getCategoryGUI().openCategoryGUI(player, category, 0);

      // Mensagem
      String message = plugin.getConfigLoader()
          .getMessage("category_opened", "&aVocê abriu a categoria &e{category}&a!")
          .replace("{category}", category.getName());
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() + message));

      return true;
    }

    return false;
  }
}
