package com.primeleague.shop.services;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

/**
 * Serviço para gerenciar a economia
 * Suporta Vault para compatibilidade com vários plugins de economia
 */
public class EconomyService {

  private final PrimeLeagueShopPlugin plugin;
  private Economy economy;
  private boolean vaultEnabled;

  /**
   * Cria um novo serviço de economia
   *
   * @param plugin Instância do plugin
   */
  public EconomyService(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.vaultEnabled = setupEconomy();
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
      plugin.getLogger().warning("Tentativa de obter saldo sem economia disponível");
      return 0.0;
    }

    return economy.getBalance(player.getName());
  }

  /**
   * Verifica se um jogador tem saldo suficiente
   *
   * @param player Jogador para verificar
   * @param amount Quantia necessária
   * @return true se o jogador tiver saldo suficiente
   */
  public boolean hasMoney(Player player, double amount) {
    if (!isEconomyAvailable()) {
      plugin.getLogger().warning("Tentativa de verificar saldo sem economia disponível");
      return false;
    }

    return economy.has(player.getName(), amount);
  }

  /**
   * Retira dinheiro de um jogador
   *
   * @param player Jogador
   * @param amount Quantia a retirar
   * @return true se a operação foi bem-sucedida
   */
  public boolean withdrawMoney(Player player, double amount) {
    if (!isEconomyAvailable()) {
      plugin.getLogger().warning("Tentativa de retirar dinheiro sem economia disponível");
      return false;
    }

    if (!economy.has(player.getName(), amount)) {
      return false;
    }

    return economy.withdrawPlayer(player.getName(), amount).transactionSuccess();
  }

  /**
   * Adiciona dinheiro a um jogador
   *
   * @param player Jogador
   * @param amount Quantia a adicionar
   * @return true se a operação foi bem-sucedida
   */
  public boolean depositMoney(Player player, double amount) {
    if (!isEconomyAvailable()) {
      plugin.getLogger().warning("Tentativa de depositar dinheiro sem economia disponível");
      return false;
    }

    return economy.depositPlayer(player.getName(), amount).transactionSuccess();
  }

  /**
   * Formata um valor monetário
   *
   * @param amount Valor
   * @return Valor formatado
   */
  public String formatMoney(double amount) {
    if (isEconomyAvailable()) {
      return economy.format(amount);
    } else {
      return String.format("%.2f%s", amount, plugin.getConfigLoader().getCurrencySymbol());
    }
  }
}
