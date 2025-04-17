package com.primeleague.shop.combat;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

public class CombatManager {
  private final PrimeLeagueShopPlugin plugin;
  private final Map<String, Long> combatTags;
  private final long combatDuration;

  public CombatManager(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.combatTags = Collections.synchronizedMap(new HashMap<String, Long>());
    this.combatDuration = plugin.getConfig().getLong("combat.duration", 10) * 1000; // Segundos para ms
  }

  public void tagPlayer(Player player) {
    String playerName = player.getName();
    combatTags.put(playerName, System.currentTimeMillis());
    player.sendMessage("§cVocê entrou em combate! Não poderá usar a loja por " +
        (combatDuration / 1000) + " segundos.");
  }

  public boolean isInCombat(Player player) {
    String playerName = player.getName();
    Long tagTime = combatTags.get(playerName);
    if (tagTime == null)
      return false;

    if (System.currentTimeMillis() - tagTime > combatDuration) {
      combatTags.remove(playerName);
      return false;
    }
    return true;
  }

  public void cleanup() {
    long now = System.currentTimeMillis();
    synchronized (combatTags) {
      combatTags.entrySet().removeIf(entry -> now - entry.getValue() > combatDuration);
    }
  }

  public void removeTag(Player player) {
    combatTags.remove(player.getName());
  }

  public long getRemainingTime(Player player) {
    String playerName = player.getName();
    Long tagTime = combatTags.get(playerName);
    if (tagTime == null)
      return 0;

    long remaining = combatDuration - (System.currentTimeMillis() - tagTime);
    return Math.max(0, remaining);
  }
}
