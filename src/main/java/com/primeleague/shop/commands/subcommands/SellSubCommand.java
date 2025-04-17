package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.entity.Player;

/**
 * Subcomando para vender itens diretamente
 */
public class SellSubCommand implements SubCommand {

  private final PrimeLeagueShopPlugin plugin;

  /**
   * Cria um novo subcomando
   *
   * @param plugin Instância do plugin
   */
  public SellSubCommand(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getName() {
    return ShopConstants.CMD_SELL;
  }

  @Override
  public boolean execute(Player player, String[] args) {
    // Verifica permissão
    if (!player.hasPermission(ShopConstants.PERM_SELL)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
      return true;
    }

    // Verifica argumento de item
    if (args.length < 2) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          "&cUso correto: /shop sell <item> [quantidade]"));
      return true;
    }

    // Obtém o nome do item
    String itemName = args[1];
    ShopItem item = plugin.getShopManager().findItemByName(itemName);

    if (item == null) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("item_not_found", "&cEste item não está disponível na loja.")));
      return true;
    }

    // Obtém a quantidade
    int quantity = 1;
    if (args.length >= 3) {
      try {
        quantity = Integer.parseInt(args[2]);

        int maxSellQuantity = plugin.getConfigLoader().getMaxSellQuantity();
        if (quantity <= 0 || quantity > maxSellQuantity) {
          player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
              plugin.getConfigLoader().getMessage("invalid_amount",
                  "&cQuantidade inválida. Use um número entre 1 e {max}.")
                  .replace("{max}", String.valueOf(maxSellQuantity))));
          return true;
        }
      } catch (NumberFormatException e) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
            "&cQuantidade inválida. Use um número inteiro."));
        return true;
      }
    }

    // Executa a venda
    plugin.getShopManager().sellItem(player, item, quantity);
    return true;
  }
}
