package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import com.primeleague.shop.models.Transaction;
import org.bukkit.entity.Player;

import java.util.List;

public class HistorySubCommand implements SubCommand {
  private final PrimeLeagueShopPlugin plugin;

  public HistorySubCommand(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getName() {
    return "history";
  }

  @Override
  public boolean execute(Player player, String[] args) {
    if (!player.hasPermission("primeleague.shop.history")) {
      player.sendMessage(plugin.getConfigLoader().getPrefix() +
          "§cVocê não tem permissão para ver o histórico.");
      return true;
    }

    // Processa argumentos (página)
    int page = 1;
    if (args.length > 1) {
      try {
        page = Integer.parseInt(args[1]);
        if (page < 1)
          page = 1;
      } catch (NumberFormatException e) {
        player.sendMessage(plugin.getConfigLoader().getPrefix() +
            "§cNúmero de página inválido.");
        return true;
      }
    }

    // Carrega histórico de forma assíncrona
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      List<Transaction> history = plugin.getTransactionHistory()
          .getPlayerHistory(player.getName(), 10);

      // Volta para a thread principal para mostrar mensagens
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (history.isEmpty()) {
          player.sendMessage(plugin.getConfigLoader().getPrefix() +
              "§eVocê ainda não realizou nenhuma transação.");
          return;
        }

        String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();
        player.sendMessage("§8=== §aHistórico de Transações §8===");
        for (Transaction transaction : history) {
          String type = transaction.getType() == Transaction.TransactionType.BUY ? "§aComprou" : "§cVendeu";
          player.sendMessage(String.format("§7%s §f%dx %s §7por §f%s%.2f",
              type,
              transaction.getQuantity(),
              transaction.getItem().getName(),
              currencySymbol,
              transaction.getTotalPrice()));
        }
      });
    });

    return true;
  }
}
