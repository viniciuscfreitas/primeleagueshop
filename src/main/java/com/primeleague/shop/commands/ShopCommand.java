package com.primeleague.shop.commands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.subcommands.*;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.models.Transaction.TransactionType;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

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
    registerSubCommand(new ListSubCommand(plugin));
    registerSubCommand(new SellAllSubCommand(plugin));
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
      return true;
    }

    Player player = (Player) sender;

    if (args.length == 0) {
      openMainShop(player);
      return true;
    }

    String subCommand = args[0].toLowerCase();

    switch (subCommand) {
      case "ajuda":
        sendUsage(player);
        break;
      case "historico":
      case "history": // Mantém compatibilidade
        handleHistory(player, args);
        break;
      case "top":
        handleTop(player, args);
        break;
      case "lista":
      case "list": // Mantém compatibilidade
        if (subCommands.containsKey("list")) {
          return subCommands.get("list").execute(player, args);
        }
        break;
      case "vender-tudo":
      case "sellall": // Mantém compatibilidade
        if (subCommands.containsKey("sellall")) {
          return subCommands.get("sellall").execute(player, args);
        }
        break;
      default:
        if (subCommands.containsKey(subCommand)) {
          return subCommands.get(subCommand).execute(player, args);
        }
        sendUsage(player);
        break;
    }

    return true;
  }

  private void openMainShop(Player player) {
    if (!player.hasPermission("primeleague.shop.use")) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          plugin.getConfigLoader().getMessage("no_permission", "§cVocê não tem permissão para isso.")));
      return;
    }
    plugin.getShopGUI().openMainMenu(player);
  }

  private void sendUsage(Player player) {
    player.sendMessage("§8=== §aComandos da Loja §8===");
    player.sendMessage("§e/loja §7- Abre a loja");
    player.sendMessage("§e/loja ajuda §7- Mostra esta mensagem");
    player.sendMessage("§e/loja historico §7- Mostra seu histórico de transações");
    player.sendMessage("§e/loja top [compras|vendas] §7- Mostra ranking de compradores/vendedores");
    if (player.hasPermission("primeleague.shop.admin")) {
      player.sendMessage("§e/loja admin §7- Comandos administrativos");
    }
  }

  private boolean handleHistory(Player player, String[] args) {
    if (!player.hasPermission("primeleague.shop.history")) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          "§cVocê não tem permissão para ver o histórico."));
      return true;
    }

    // Processa argumentos (página)
    int page = 1;
    if (args.length > 1) {
      try {
        page = Integer.parseInt(args[1]);
        if (page < 1) {
          player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
              "§cNúmero de página inválido."));
          return true;
        }
      } catch (NumberFormatException e) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
            "§cNúmero de página inválido."));
        return true;
      }
    }

    final int itemsPerPage = 10;
    final int pageNumber = page;

    // Carrega histórico de forma assíncrona
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      List<Transaction> allHistory = plugin.getTransactionHistory()
          .getPlayerHistory(player.getName(), 100); // Pega mais transações para paginação

      // Calcula total de páginas
      int totalPages = (int) Math.ceil((double) allHistory.size() / itemsPerPage);

      // Ajusta página se necessário
      if (pageNumber > totalPages) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
          player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
              "§cPágina inválida! Total de páginas: §f" + totalPages));
        });
        return;
      }

      // Calcula índices da página
      int startIndex = (pageNumber - 1) * itemsPerPage;
      int endIndex = Math.min(startIndex + itemsPerPage, allHistory.size());
      List<Transaction> pageHistory = allHistory.subList(startIndex, endIndex);

      // Volta para a thread principal para mostrar mensagens
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (allHistory.isEmpty()) {
          player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
              "§eVocê ainda não realizou nenhuma transação."));
          return;
        }

        String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();
        player.sendMessage("§8=== §aHistórico de Transações (Página " + pageNumber + "/" + totalPages + ") §8===");

        for (Transaction transaction : pageHistory) {
          String type = transaction.getType() == Transaction.TransactionType.BUY ? "§aComprou" : "§cVendeu";
          player.sendMessage(String.format("§7%s §f%dx %s §7por §f%s%.2f",
              type,
              transaction.getQuantity(),
              transaction.getItemName(),
              currencySymbol,
              transaction.getTotalPrice()));
        }

        // Mostra navegação
        if (totalPages > 1) {
          StringBuilder nav = new StringBuilder("§7");
          if (pageNumber > 1) {
            nav.append("§a/loja historico ").append(pageNumber - 1).append(" §7<<< ");
          }
          nav.append("§fPágina ").append(pageNumber).append("/").append(totalPages);
          if (pageNumber < totalPages) {
            nav.append(" §7>>> §a/loja historico ").append(pageNumber + 1);
          }
          player.sendMessage(TextUtils.colorize(nav.toString()));
        }
      });
    });

    return true;
  }

  private boolean handleTop(Player player, String[] args) {
    if (!player.hasPermission("primeleague.shop.top")) {
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
          "§cVocê não tem permissão para ver o ranking."));
      return true;
    }

    // Processa argumentos (tipo de ranking)
    String type = args.length > 1 ? args[1].toLowerCase() : "compras";
    boolean isBuyers = !type.equals("vendas");

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
