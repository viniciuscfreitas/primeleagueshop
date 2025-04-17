package com.primeleague.shop.listeners;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {
  private final PrimeLeagueShopPlugin plugin;

  public CombatListener(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    // Verifica se a vítima é um jogador
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    Player victim = (Player) event.getEntity();

    // Identifica o atacante
    Player attacker = null;
    if (event.getDamager() instanceof Player) {
      attacker = (Player) event.getDamager();
    } else if (event.getDamager() instanceof Projectile) {
      Projectile projectile = (Projectile) event.getDamager();
      if (projectile.getShooter() instanceof Player) {
        attacker = (Player) projectile.getShooter();
      }
    }

    // Se encontrou um atacante válido, marca ambos em combate
    if (attacker != null && attacker != victim) {
      plugin.getCombatManager().tagPlayer(victim);
      plugin.getCombatManager().tagPlayer(attacker);
    }
  }
}
