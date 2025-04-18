package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class HistorySubCommand implements SubCommand {
    private final PrimeLeagueShopPlugin plugin;

    public HistorySubCommand(PrimeLeagueShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "historico";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (!player.hasPermission("primeleagueshop.history")) {
            player.sendMessage(plugin.getConfigLoader().getPrefix() + "§cVocê não tem permissão para ver o histórico.");
            return true;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Transaction> transactions = plugin.getTransactionHistory().getPlayerHistory(player.getName(), 10);

            if (transactions.isEmpty()) {
                player.sendMessage(TextUtils.colorize("&eVocê ainda não realizou nenhuma transação."));
                return;
            }

            player.sendMessage(TextUtils.colorize("&e&lSuas últimas transações:"));
            for (Transaction t : transactions) {
                String type = t.isBuy() ? "&aComprou" : "&cVendeu";
                player.sendMessage(TextUtils.colorize(String.format(
                    "%s &f%dx %s por &6$%.2f",
                    type,
                    t.getQuantity(),
                    t.getItemName(),
                    t.getPrice()
                )));
            }
        });
        return true;
    }
}
