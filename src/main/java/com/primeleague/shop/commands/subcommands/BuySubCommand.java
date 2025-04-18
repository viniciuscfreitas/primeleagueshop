package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Subcomando para comprar itens diretamente
 */
public class BuySubCommand implements SubCommand {

  private final PrimeLeagueShopPlugin plugin;

  /**
   * Cria um novo subcomando
   *
   * @param plugin Instância do plugin
   */
  public BuySubCommand(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getName() {
    return ShopConstants.CMD_BUY;
  }

  @Override
  public boolean execute(Player player, String[] args) {
    if (!player.hasPermission(ShopConstants.PERM_BUY)) {
      player.sendMessage(TextUtils.colorize("&cVocê não tem permissão para isso!"));
      return true;
    }

    if (args.length < 2) {
      player.sendMessage(TextUtils.colorize("&cUso correto: /shop buy <item> <quantidade>"));
      return true;
    }

    // Junta todos os argumentos do nome do item até encontrar um número
    StringBuilder itemNameBuilder = new StringBuilder();
    int quantityIndex = -1;

    for (int i = 1; i < args.length; i++) {
      try {
        int testNumber = Integer.parseInt(args[i]);
        quantityIndex = i;
        break;
      } catch (NumberFormatException e) {
        if (itemNameBuilder.length() > 0) {
          itemNameBuilder.append(" ");
        }
        itemNameBuilder.append(args[i]);
      }
    }

    String itemName = itemNameBuilder.toString();
    ShopItem item = null;

    try {
      int materialId = Integer.parseInt(itemName);
      Material material;
      if (materialId == 276) {
        material = Material.DIAMOND_SWORD;
      } else {
        material = Material.getMaterial(materialId);
      }
      if (material != null) {
        item = plugin.getShopManager().getItemByMaterialAndData(material.name(), (byte) 0);
      }
    } catch (NumberFormatException e) {
      item = plugin.getShopManager().findItemByName(itemName);
    }

    if (item == null) {
      player.sendMessage(TextUtils.colorize("&cItem não encontrado na loja! Use /shop list para ver os itens disponíveis."));
      return true;
    }

    int quantity = 1;
    if (quantityIndex != -1 && quantityIndex < args.length) {
      try {
        quantity = Integer.parseInt(args[quantityIndex]);
        if (quantity <= 0) throw new NumberFormatException();
      } catch (NumberFormatException e) {
        player.sendMessage(TextUtils.colorize("&cA quantidade deve ser um número positivo!"));
        return true;
      }
    }

    // Mostra uma mensagem de confirmação antes de abrir a GUI
    player.sendMessage(TextUtils.colorize("&aAbrindo confirmação de compra para &f" +
        item.getName() + " &a(Preço: &f" +
        String.format("%.2f", item.getBuyPrice()) +
        plugin.getConfigLoader().getCurrencySymbol() + "&a)"));

    plugin.getConfirmationGUI().openBuyConfirmation(player, item, true);
    return true;
  }
}
