package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TopSubCommand implements SubCommand {
  private final PrimeLeagueShopPlugin plugin;
  private final SimpleDateFormat dateFormat;

  public TopSubCommand(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
  }

  @Override
  public String getName() {
    return "top";
  }

  @Override
  public boolean execute(Player player, String[] args) {
    if (!player.hasPermission("primeleague.shop.top")) {
      player.sendMessage(plugin.getConfigLoader().getPrefix() +
          "§cVocê não tem permissão para ver o ranking.");
      return true;
    }

    // Processa argumentos (tipo de ranking)
    String type = args.length > 1 ? args[1].toLowerCase() : "buy";
    boolean isBuyers = !type.equals("sell");

    // Carrega ranking de forma assíncrona
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      List<Map.Entry<String, Double>> top = isBuyers ? plugin.getRankingManager().getTopBuyers(10)
          : plugin.getRankingManager().getTopSellers(10);

      // Volta para a thread principal para mostrar mensagens
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        String lastReset = dateFormat.format(
            new Date(plugin.getRankingManager().getLastReset()));

        player.sendMessage("§8=== §aRanking Semanal - " +
            (isBuyers ? "Compradores" : "Vendedores") + " §8===");
        player.sendMessage("§7Última atualização: §f" + lastReset);

        if (top.isEmpty()) {
          player.sendMessage("§eNenhuma transação registrada ainda.");
          return;
        }

        int position = 1;
        for (Map.Entry<String, Double> entry : top) {
          String medal = position == 1 ? "§6" : position == 2 ? "§7" : position == 3 ? "§c" : "§f";
          player.sendMessage(String.format("%s#%d §7%s: §f$%.2f",
              medal, position, entry.getKey(), entry.getValue()));
          position++;
        }
      });
    });

    return true;
  }
}
