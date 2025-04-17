package com.primeleague.shop.listeners;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
  private final PrimeLeagueShopPlugin plugin;

  public PlayerListener(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    String playerName = event.getPlayer().getName();

    // Limpa caches do jogador
    plugin.getShopManager().cleanupPlayerData(playerName);
    plugin.getPreferencesManager().cleanupPlayerData(playerName);

    // Agenda limpeza completa para próximo ciclo
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      plugin.getShopManager().runCacheCleanup();
      plugin.getPreferencesManager().runCacheCleanup();
    }, 100L); // 5 segundos depois
  }
}
