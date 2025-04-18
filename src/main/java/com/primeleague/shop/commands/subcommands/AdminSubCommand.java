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
    player.sendMessage(TextUtils.colorize("&aComandos administrativos:"));
    player.sendMessage(TextUtils.colorize("  &f/loja &7- Abre a loja"));
    player.sendMessage(TextUtils.colorize("  &f/loja comprar <item> [qtd] &7- Compra um item"));
    player.sendMessage(TextUtils.colorize("  &f/loja vender <item> [qtd] &7- Vende um item"));
    player.sendMessage(TextUtils.colorize("  &f/loja vender-tudo &7- Vende todos os itens do inventário"));
    player.sendMessage(TextUtils.colorize("  &f/loja recarregar &7- Recarrega as configurações"));
    player.sendMessage(TextUtils.colorize("  &f/loja historico [página] &7- Mostra histórico de transações"));
    player.sendMessage(TextUtils.colorize("  &f/loja top [compras|vendas] &7- Mostra ranking de transações"));
    player.sendMessage(TextUtils.colorize("  &f/loja lista [página] &7- Lista todos os itens disponíveis"));
    player.sendMessage(TextUtils.colorize("&aInformações do sistema:"));
    player.sendMessage(TextUtils.colorize("  &7- &fMoeda: &a" + plugin.getConfigLoader().getCurrencySymbol()));
    player.sendMessage(TextUtils.colorize("  &7- &fLimite de venda: &a" + plugin.getConfigLoader().getMaxSellQuantity()));
    player.sendMessage(TextUtils.colorize("&8&m--------------------------------------------------------"));

    return true;
  }
}
