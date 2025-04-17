package com.primeleague.shop.services;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.utils.ShopConstants;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.logging.Level;

public class FeedbackManager {
  private final PrimeLeagueShopPlugin plugin;

  public FeedbackManager(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
  }

  public void sendPurchaseFeedback(Player player, boolean success) {
    if (success) {
      // Som de compra
      playSound(player, "purchase");

      // Partículas
      spawnParticles(player.getLocation(), "purchase");

      // Mensagem
      sendActionBar(player,
          plugin.getConfigLoader().getMessage("feedback.actionbar.purchase", "Compra realizada com sucesso!"));
    } else {
      // Feedback de erro
      playSound(player, "error");
      spawnParticles(player.getLocation(), "error");
      // Mensagem de erro será enviada pelo serviço que detectou o erro
    }
  }

  public void sendSellFeedback(Player player, boolean success) {
    if (success) {
      playSound(player, "sell");
      spawnParticles(player.getLocation(), "sell");
      sendActionBar(player,
          plugin.getConfigLoader().getMessage("feedback.actionbar.sell", "Venda realizada com sucesso!"));
    } else {
      playSound(player, "error");
      spawnParticles(player.getLocation(), "error");
    }
  }

  private void playSound(Player player, String type) {
    try {
      String soundName = plugin.getConfigLoader().getMessage("feedback.sounds." + type, "LEVEL_UP");
      Sound sound = Sound.valueOf(soundName);
      player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING,
          String.format(ShopConstants.LOG_FEEDBACK_ERROR, "Som inválido na configuração: " + type));
      // Som inválido na config, usa som padrão
      player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
    }
  }

  private void spawnParticles(Location location, String type) {
    try {
      String effectName = plugin.getConfigLoader().getMessage("feedback.particles." + type, "HAPPY_VILLAGER");
      Effect effect = Effect.valueOf(effectName);
      location.getWorld().playEffect(location.add(0, 1, 0), effect, 0);
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING,
          String.format(ShopConstants.LOG_FEEDBACK_ERROR, "Efeito inválido na configuração: " + type));
      // Efeito inválido na config, ignora
    }
  }

  private void sendActionBar(Player player, String message) {
    // No Spigot 1.5.2 não existe ActionBar
    // Envia mensagem normal no chat
    player.sendMessage(message);
  }

  public void sendErrorFeedback(Player player, String message) {
    playSound(player, "error");
    spawnParticles(player.getLocation(), "error");
    player.sendMessage(message);
  }
}
