package com.primeleague.shop.services;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopItem;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;

public class DynamicPricingService {
  private final PrimeLeagueShopPlugin plugin;
  private final Map<String, PriceData> priceCache;
  private final double minMultiplier;
  private final double maxMultiplier;
  private final double decayRate;
  private final Logger logger;
  private boolean enabled;

  private static class PriceData {
    private double basePrice;
    private double currentPrice;
    private int demand;
    private long lastUpdate;

    public PriceData(double basePrice) {
      this.basePrice = basePrice;
      this.currentPrice = basePrice;
      this.demand = 0;
      this.lastUpdate = System.currentTimeMillis();
    }
  }

  public DynamicPricingService(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.priceCache = new HashMap<String, PriceData>();
    this.logger = plugin.getLogger();

    ConfigurationSection config = plugin.getConfig().getConfigurationSection("pricing.dynamic");
    if (config == null) {
      logger.warning("Seção 'pricing.dynamic' não encontrada no config.yml. Usando valores padrão.");
      this.enabled = false;
      this.minMultiplier = 0.5;
      this.maxMultiplier = 2.0;
      this.decayRate = 0.01;
      return;
    }

    this.enabled = config.getBoolean("enabled", false);
    this.minMultiplier = config.getDouble("min-multiplier", 0.5);
    this.maxMultiplier = config.getDouble("max-multiplier", 2.0);
    this.decayRate = config.getDouble("decay-rate", 0.01);

    logger.info("Sistema de preços dinâmicos inicializado:");
    logger.info("Enabled: " + enabled);
    logger.info("Min Multiplier: " + minMultiplier);
    logger.info("Max Multiplier: " + maxMultiplier);
    logger.info("Decay Rate: " + decayRate);

    if (enabled) {
      startPriceUpdateTask();
    }
  }

  public double getCurrentPrice(ShopItem item) {
    if (!enabled) {
      return item.getBuyPrice();
    }

    String itemId = item.getMaterial().name() + ":" + item.getData();
    PriceData data = priceCache.get(itemId);

    if (data == null) {
      data = new PriceData(item.getBuyPrice());
      priceCache.put(itemId, data);
      logger.fine("Novo item adicionado ao cache de preços: " + itemId + " com preço base: " + data.basePrice);
      return data.currentPrice;
    }

    return data.currentPrice;
  }

  public void updateDemand(ShopItem item, int quantity) {
    if (!enabled) {
      return;
    }

    String itemId = item.getMaterial().name() + ":" + item.getData();
    PriceData data = priceCache.get(itemId);

    if (data == null) {
      data = new PriceData(item.getBuyPrice());
      priceCache.put(itemId, data);
    }

    data.demand += quantity;
    updatePrice(itemId, data);
    logger.fine("Demanda atualizada para " + itemId + ": " + data.demand + ", novo preço: " + data.currentPrice);
  }

  private void updatePrice(String itemId, PriceData data) {
    try {
      double multiplier = 1.0 + (data.demand * decayRate);
      multiplier = Math.max(minMultiplier, Math.min(maxMultiplier, multiplier));

      double oldPrice = data.currentPrice;
      data.currentPrice = data.basePrice * multiplier;
      data.lastUpdate = System.currentTimeMillis();

      if (Math.abs(oldPrice - data.currentPrice) > 0.01) {
        logger.fine("Preço atualizado para " + itemId + ": " + oldPrice + " -> " + data.currentPrice);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Erro ao atualizar preço para " + itemId, e);
    }
  }

  private void startPriceUpdateTask() {
    int interval = plugin.getConfig().getInt("pricing.dynamic.update-interval", 300) * 20;
    logger.info("Iniciando tarefa de atualização de preços com intervalo de " + interval + " ticks");

    plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
      public void run() {
        try {
          int updatedItems = 0;
          for (Map.Entry<String, PriceData> entry : priceCache.entrySet()) {
            PriceData data = entry.getValue();
            if (data.demand > 0) {
              data.demand = Math.max(0, data.demand - 1);
              updatePrice(entry.getKey(), data);
              updatedItems++;
            }
          }
          if (updatedItems > 0) {
            logger.fine("Atualizados " + updatedItems + " itens no ciclo de atualização de preços");
          }
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Erro durante a atualização automática de preços", e);
        }
      }
    }, interval, interval);
  }

  public void shutdown() {
    logger.info("Desligando serviço de preços dinâmicos, limpando " + priceCache.size() + " itens do cache");
    priceCache.clear();
  }

  public boolean isEnabled() {
    return enabled;
  }
}
