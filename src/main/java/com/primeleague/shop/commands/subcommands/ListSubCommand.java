package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import com.primeleague.shop.models.ShopCategory;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Subcomando para listar itens disponíveis na loja
 */
public class ListSubCommand implements SubCommand {

  private final PrimeLeagueShopPlugin plugin;

  /**
   * Cria um novo subcomando
   *
   * @param plugin Instância do plugin
   */
  public ListSubCommand(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getName() {
    return "list";
  }

  @Override
  public boolean execute(Player player, String[] args) {
    if (!player.hasPermission(ShopConstants.PERM_BUY)) {
      player.sendMessage(TextUtils.colorize("&cVocê não tem permissão para isso!"));
      return true;
    }

    // Listar todas as categorias e itens
    List<ShopCategory> categories = plugin.getShopManager().getCategories();

    if (categories.isEmpty()) {
      player.sendMessage(TextUtils.colorize("&cNenhuma categoria encontrada!"));
      return true;
    }

    player.sendMessage(TextUtils.colorize("&a=== Itens disponíveis na loja ==="));

    for (ShopCategory category : categories) {
      player.sendMessage(TextUtils.colorize("&e" + category.getName() + ":"));

      for (ShopItem item : category.getItems()) {
        String buyPrice = item.getBuyPrice() > 0 ? String.format("%.2f", item.getBuyPrice()) : "N/A";
        String sellPrice = item.getSellPrice() > 0 ? String.format("%.2f", item.getSellPrice()) : "N/A";

        player.sendMessage(TextUtils.colorize("  &7- &f" + item.getName() +
            " &7(Compra: &f" + buyPrice + plugin.getConfigLoader().getCurrencySymbol() +
            "&7, Venda: &f" + sellPrice + plugin.getConfigLoader().getCurrencySymbol() + "&7)"));
      }
    }

    return true;
  }
}
