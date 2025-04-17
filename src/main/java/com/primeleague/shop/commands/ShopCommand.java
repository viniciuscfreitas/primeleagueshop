package com.primeleague.shop.commands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.subcommands.*;
import com.primeleague.shop.gui.ShopGUI;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import com.primeleague.shop.models.Transaction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Comando principal da loja
 */
public class ShopCommand implements CommandExecutor {

  private final PrimeLeagueShopPlugin plugin;
  private final Map<String, SubCommand> subCommands;
  private final SimpleDateFormat dateFormat;

  /**
   * Cria o comando principal
   *
   * @param plugin Instância do plugin
   */
  public ShopCommand(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.subCommands = new HashMap<>();
    this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Registra subcomandos
    registerSubCommand(new BuySubCommand(plugin));
    registerSubCommand(new SellSubCommand(plugin));
    registerSubCommand(new ReloadSubCommand(plugin));
    registerSubCommand(new AdminSubCommand(plugin));
    registerSubCommand(new HistorySubCommand(plugin));
    registerSubCommand(new TopSubCommand(plugin));
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
      return true;
    }

    Player player = (Player) sender;

    // Se não houver argumentos, abre a loja
    if (args.length == 0) {
      if (!player.hasPermission("primeleague.shop.use")) {
        player.sendMessage(plugin.getConfigLoader().getPrefix() +
            plugin.getConfigLoader().getMessage("no_permission", "§cVocê não tem permissão para isso."));
        return true;
      }
      plugin.getShopGUI().openMainMenu(player);
      return true;
    }

    // Processa subcomandos
    String subCommand = args[0].toLowerCase();
    if (subCommands.containsKey(subCommand)) {
      return subCommands.get(subCommand).execute(player, args);
    }

    sendUsage(player);
    return true;
  }

  private boolean handleHistory(Player player, String[] args) {
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

        player.sendMessage("§8=== §aHistórico de Transações §8===");
        for (Transaction transaction : history) {
          String type = transaction.getType() == Transaction.TransactionType.BUY ? "§aComprou" : "§cVendeu";
          player.sendMessage(String.format("§7%s §f%dx %s §7por §f$%.2f",
              type,
              transaction.getQuantity(),
              transaction.getItem().getName(),
              transaction.getTotalPrice()));
        }
      });
    });

    return true;
  }

  private boolean handleTop(Player player, String[] args) {
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

  private void sendUsage(Player player) {
    player.sendMessage("§e/shop §7- Abre a loja");
    player.sendMessage("§e/shop history §7- Mostra histórico de transações");
    player.sendMessage("§e/shop top [buy|sell] §7- Mostra ranking de compradores/vendedores");
  }

  /**
   * Registra um subcomando
   *
   * @param subCommand Subcomando a registrar
   */
  private void registerSubCommand(SubCommand subCommand) {
    subCommands.put(subCommand.getName().toLowerCase(), subCommand);
  }

  /**
   * Interface para subcomandos
   */
  public interface SubCommand {
    /**
     * Obtém o nome do subcomando
     *
     * @return Nome do subcomando
     */
    String getName();

    /**
     * Executa o subcomando
     *
     * @param player Jogador que executou
     * @param args   Argumentos do comando
     * @return true se o comando foi executado com sucesso
     */
    boolean execute(Player player, String[] args);
  }
}
