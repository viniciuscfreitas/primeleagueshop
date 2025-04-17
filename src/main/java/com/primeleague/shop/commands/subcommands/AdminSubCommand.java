package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.entity.Player;

/**
 * Subcomando para funções administrativas
 */
public class AdminSubCommand implements SubCommand {

  private final PrimeLeagueShopPlugin plugin;

  /**
   * Cria um novo subcomando
   *
   * @param plugin Instância do plugin
   */
  public AdminSubCommand(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getName() {
    return ShopConstants.CMD_ADMIN;
  }

  @Override
  public boolean execute(Player player, String[] args) {
    // Verifica permissão
    if (!player.hasPermission(ShopConstants.PERM_ADMIN)) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
      return true;
    }

    // Exibe informações administrativas
    player.sendMessage(TextUtils.colorize("&8&m---------------------&r &aLoja Prime League &8&m---------------------"));
    player.sendMessage(TextUtils.colorize("&aVersão: &f" + plugin.getDescription().getVersion()));
    player.sendMessage(TextUtils.colorize("&aCategorias: &f" + plugin.getShopManager().getCategories().size()));
    player.sendMessage(TextUtils
        .colorize("&aEconomia: &f" + (plugin.getEconomyService().isEconomyAvailable() ? "Conectada" : "Desconectada")));
    player.sendMessage(TextUtils.colorize("&aComandos disponíveis:"));
    player.sendMessage(TextUtils.colorize("  &f/shop &7- Abre a loja"));
    player.sendMessage(TextUtils.colorize("  &f/shop buy <item> [qtd] &7- Compra um item"));
    player.sendMessage(TextUtils.colorize("  &f/shop sell <item> [qtd] &7- Vende um item"));
    player.sendMessage(TextUtils.colorize("  &f/shop reload &7- Recarrega as configurações"));
    player.sendMessage(TextUtils.colorize("&8&m--------------------------------------------------------"));

    return true;
  }
}
