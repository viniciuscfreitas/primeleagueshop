package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import com.primeleague.shop.models.ShopCategory;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;

/**
 * Subcomando para listar itens disponíveis na loja
 */
public class ListSubCommand implements SubCommand {

  private final PrimeLeagueShopPlugin plugin;
  private static final int ITEMS_PER_PAGE = 10;

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
    return ShopConstants.CMD_LIST;
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

    // Cria uma lista com todos os itens e suas categorias
    List<ItemDisplay> allItems = new ArrayList<>();
    for (ShopCategory category : categories) {
      for (ShopItem item : category.getItems()) {
        allItems.add(new ItemDisplay(category.getName(), item));
      }
    }

    // Calcula o número total de páginas
    int totalPages = (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE);

    // Obtém a página desejada
    int page = 1;
    if (args.length > 1) {
      try {
        page = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
        player.sendMessage(TextUtils.colorize("&cNúmero de página inválido!"));
        return true;
      }
    }

    // Valida a página
    if (page < 1 || page > totalPages) {
      player.sendMessage(TextUtils.colorize("&cPágina inválida! Total de páginas: &f" + totalPages));
      return true;
    }

    // Calcula o início e fim da página
    int startIndex = (page - 1) * ITEMS_PER_PAGE;
    int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

    // Mostra o cabeçalho
    player.sendMessage(TextUtils.colorize("&a=== Itens disponíveis na loja (Página " + page + "/" + totalPages + ") ==="));

    // Mostra os itens da página atual
    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();
    String lastCategory = null;

    for (int i = startIndex; i < endIndex; i++) {
      ItemDisplay display = allItems.get(i);

      // Mostra o nome da categoria apenas quando muda
      if (lastCategory == null || !lastCategory.equals(display.category)) {
        player.sendMessage(TextUtils.colorize("&e" + display.category + ":"));
        lastCategory = display.category;
      }

      String buyPrice = display.item.getBuyPrice() > 0 ? String.format("%.2f", display.item.getBuyPrice()) : "N/A";
      String sellPrice = display.item.getSellPrice() > 0 ? String.format("%.2f", display.item.getSellPrice()) : "N/A";

      player.sendMessage(TextUtils.colorize("  &7- &f" + display.item.getName() +
          " &7(Compra: &f" + buyPrice + currencySymbol +
          "&7, Venda: &f" + sellPrice + currencySymbol + "&7)"));
    }

    // Mostra navegação
    if (totalPages > 1) {
      StringBuilder nav = new StringBuilder("&7");
      if (page > 1) {
        nav.append("&a/loja lista ").append(page - 1).append(" &7<<< ");
      }
      nav.append("&fPágina ").append(page).append("/").append(totalPages);
      if (page < totalPages) {
        nav.append(" &7>>> &a/loja lista ").append(page + 1);
      }
      player.sendMessage(TextUtils.colorize(nav.toString()));
    }

    return true;
  }

  private static class ItemDisplay {
    final String category;
    final ShopItem item;

    ItemDisplay(String category, ShopItem item) {
      this.category = category;
      this.item = item;
    }
  }
}
