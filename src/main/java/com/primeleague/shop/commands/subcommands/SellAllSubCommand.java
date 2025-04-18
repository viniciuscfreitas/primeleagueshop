package com.primeleague.shop.commands.subcommands;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.commands.ShopCommand.SubCommand;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.models.Transaction;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Subcomando para vender todos os itens do inventário
 */
public class SellAllSubCommand implements SubCommand {

    private final PrimeLeagueShopPlugin plugin;

    public SellAllSubCommand(PrimeLeagueShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "sellall";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (!player.hasPermission(ShopConstants.PERM_SELL)) {
            player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
                plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
            return true;
        }

        // Mapeia os itens e suas quantidades
        Map<ShopItem, Integer> itemsToSell = new HashMap<>();
        double totalValue = 0.0;

        // Verifica todos os itens no inventário
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            // Tenta encontrar o item na loja
            ShopItem shopItem = plugin.getShopManager().getItemByMaterialAndData(
                item.getType().name(),
                item.getData().getData()
            );

            if (shopItem != null && shopItem.getSellPrice() > 0) {
                itemsToSell.put(shopItem, itemsToSell.getOrDefault(shopItem, 0) + item.getAmount());
                totalValue += shopItem.getSellPrice() * item.getAmount();
            }
        }

        if (itemsToSell.isEmpty()) {
            player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
                "&cVocê não tem nenhum item que possa ser vendido na loja."));
            return true;
        }

        // Mostra confirmação
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
            "&aVendendo todos os itens disponíveis:"));

        // Processa cada item
        for (Map.Entry<ShopItem, Integer> entry : itemsToSell.entrySet()) {
            ShopItem shopItem = entry.getKey();
            int quantity = entry.getValue();
            double itemTotal = shopItem.getSellPrice() * quantity;

            // Remove os itens do inventário
            int remaining = quantity;
            ItemStack[] contents = player.getInventory().getContents();

            for (int i = 0; i < contents.length && remaining > 0; i++) {
                ItemStack invItem = contents[i];
                if (invItem != null && shopItem.matches(invItem)) {
                    int toRemove = Math.min(remaining, invItem.getAmount());
                    remaining -= toRemove;

                    if (toRemove == invItem.getAmount()) {
                        contents[i] = null;
                    } else {
                        invItem.setAmount(invItem.getAmount() - toRemove);
                    }
                }
            }

            player.getInventory().setContents(contents);

            // Registra a transação
            Transaction transaction = new Transaction(
                player.getName(),
                shopItem,
                quantity,
                shopItem.getSellPrice(),
                Transaction.TransactionType.SELL,
                new Timestamp(System.currentTimeMillis())
            );
            transaction.markSuccessful();
            plugin.getTransactionHistory().addTransaction(transaction);

            // Atualiza o ranking
            plugin.getRankingManager().updateStats(
                player.getName(),
                itemTotal,
                false
            );

            // Mostra mensagem para cada item vendido
            player.sendMessage(TextUtils.colorize(String.format(
                "&7- &f%dx %s &7por &f%s%.2f",
                quantity,
                shopItem.getName(),
                plugin.getConfigLoader().getCurrencySymbol(),
                itemTotal
            )));
        }

        // Adiciona o dinheiro ao jogador
        plugin.getEconomy().depositPlayer(player.getName(), totalValue);

        // Mostra o total
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
            String.format("&aTotal recebido: &f%s%.2f",
                plugin.getConfigLoader().getCurrencySymbol(),
                totalValue)));

        return true;
    }
}
