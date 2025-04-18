package com.primeleague.shop.services;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço para gerenciar a economia
 * Suporta Vault para compatibilidade com vários plugins de economia
 */
public class EconomyService {

  private final PrimeLeagueShopPlugin plugin;
  private Economy economy;
  private boolean vaultEnabled;

  private final Map<String, BalanceCache> balanceCache;
  private static final long CACHE_TTL = 30000; // 30 segundos
  private static final int MAX_CACHE_SIZE = 1000;

  private static class BalanceCache {
    private final double balance;
    private final long timestamp;

    public BalanceCache(double balance) {
      this.balance = balance;
      this.timestamp = System.currentTimeMillis();
    }

    public boolean isExpired() {
      return System.currentTimeMillis() - timestamp > CACHE_TTL;
    }
  }

  /**
   * Cria um novo serviço de economia
   *
   * @param plugin Instância do plugin
   */
  public EconomyService(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.balanceCache = new HashMap<>();
    this.vaultEnabled = setupEconomy();

    // Inicia tarefa de limpeza do cache
    if (vaultEnabled) {
      plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
        @Override
        public void run() {
          cleanupCache();
        }
      }, 6000L, 6000L); // A cada 5 minutos
    }
  }

  /**
   * Configura o sistema de economia (Vault)
   *
   * @return true se configurou com sucesso
   */
  private boolean setupEconomy() {
    if (!plugin.getConfigLoader().useVault()) {
      plugin.getLogger().warning("Vault desativado na configuração, não será usado.");
      return false;
    }

    if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
      plugin.getLogger().warning("Vault não encontrado, economia não estará disponível.");
      return false;
    }

    RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      plugin.getLogger().warning("Serviço de economia não encontrado, verifique se está usando um plugin de economia.");
      return false;
    }

    economy = rsp.getProvider();
    return true;
  }

  /**
   * Verifica se o sistema de economia está disponível
   *
   * @return true se o sistema estiver disponível
   */
  public boolean isEconomyAvailable() {
    return vaultEnabled && economy != null;
  }

  /**
   * Obtém o saldo de um jogador
   *
   * @param player Jogador para verificar saldo
   * @return Saldo do jogador
   */
  public double getBalance(Player player) {
    if (!isEconomyAvailable()) {
      return 0.0;
    }

    String playerName = player.getName();
    BalanceCache cached = balanceCache.get(playerName);

    if (cached != null && !cached.isExpired()) {
      return cached.balance;
    }

    double balance = economy.getBalance(playerName);

    // Atualiza cache
    balanceCache.put(playerName, new BalanceCache(balance));

    // Remove entradas antigas se o cache estiver muito grande
    if (balanceCache.size() > MAX_CACHE_SIZE) {
      cleanupCache();
    }

    return balance;
  }

  /**
   * Verifica se um jogador tem saldo suficiente
   *
   * @param player Jogador para verificar
   * @param amount Quantia necessária
   * @return true se o jogador tiver saldo suficiente
   */
  public boolean hasMoney(Player player, double amount) {
    return has(player, amount);
  }

  /**
   * Retira dinheiro de um jogador
   *
   * @param player Jogador
   * @param amount Quantia a retirar
   * @return true se a operação foi bem-sucedida
   */
  public boolean withdrawMoney(Player player, double amount) {
    return withdrawPlayer(player, amount);
  }

  /**
   * Adiciona dinheiro a um jogador
   *
   * @param player Jogador
   * @param amount Quantia a adicionar
   * @return true se a operação foi bem-sucedida
   */
  public boolean depositMoney(Player player, double amount) {
    return depositPlayer(player, amount);
  }

  /**
   * Verifica se um jogador tem saldo suficiente
   *
   * @param player Jogador para verificar
   * @param amount Quantia necessária
   * @return true se o jogador tiver saldo suficiente
   */
  public boolean has(Player player, double amount) {
    if (!isEconomyAvailable()) {
      return false;
    }

    // Usa o cache para verificação rápida
    double cachedBalance = getBalance(player);
    if (cachedBalance >= amount) {
      return true;
    }

    // Se o cache indicar que não tem saldo suficiente, verifica direto na economia
    return economy.has(player.getName(), amount);
  }

  /**
   * Retira dinheiro de um jogador
   *
   * @param player Jogador
   * @param amount Quantia a retirar
   * @return true se a operação foi bem-sucedida
   */
  public boolean withdrawPlayer(Player player, double amount) {
    if (!isEconomyAvailable() || !has(player, amount)) {
      return false;
    }

    boolean success = economy.withdrawPlayer(player.getName(), amount).transactionSuccess();
    if (success) {
      // Atualiza cache
      String playerName = player.getName();
      BalanceCache cached = balanceCache.get(playerName);
      if (cached != null && !cached.isExpired()) {
        balanceCache.put(playerName, new BalanceCache(cached.balance - amount));
      }
    }
    return success;
  }

  /**
   * Adiciona dinheiro a um jogador
   *
   * @param player Jogador
   * @param amount Quantia a adicionar
   * @return true se a operação foi bem-sucedida
   */
  public boolean depositPlayer(Player player, double amount) {
    if (!isEconomyAvailable()) {
      return false;
    }

    boolean success = economy.depositPlayer(player.getName(), amount).transactionSuccess();
    if (success) {
      // Atualiza cache
      String playerName = player.getName();
      BalanceCache cached = balanceCache.get(playerName);
      if (cached != null && !cached.isExpired()) {
        balanceCache.put(playerName, new BalanceCache(cached.balance + amount));
      }
    }
    return success;
  }

  private void cleanupCache() {
    balanceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  /**
   * Formata um valor monetário
   *
   * @param amount Valor
   * @return Valor formatado
   */
  public String format(double amount) {
    if (!isEconomyAvailable()) {
      return String.format("%.2f", amount);
    }
    return economy.format(amount);
  }
}
