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

  /**
   * Atualiza o preço de um item baseado na demanda
   */
  public void updatePrice(ShopItem item, int quantity, boolean isBuy) {
    if (!enabled) {
      return;
    }

    String itemKey = item.getMaterial().name() + ":" + item.getData();
    PriceData priceData = priceCache.computeIfAbsent(itemKey, k -> new PriceData(item.getBuyPrice()));

    // Atualiza demanda
    if (isBuy) {
      priceData.demand += quantity;
    } else {
      priceData.demand -= quantity;
    }

    // Calcula novo multiplicador
    double multiplier = 1.0;
    if (priceData.demand > 0) {
      multiplier = Math.min(maxMultiplier, 1.0 + (priceData.demand * 0.01));
    } else if (priceData.demand < 0) {
      multiplier = Math.max(minMultiplier, 1.0 + (priceData.demand * 0.01));
    }

    // Aplica decay se necessário
    long now = System.currentTimeMillis();
    long timeDiff = now - priceData.lastUpdate;
    if (timeDiff > 3600000) { // 1 hora
      int decayHours = (int) (timeDiff / 3600000);
      priceData.demand = (int) (priceData.demand * Math.pow(1.0 - decayRate, decayHours));
    }

    // Atualiza preço
    priceData.currentPrice = priceData.basePrice * multiplier;
    priceData.lastUpdate = now;

    // Atualiza cache
    priceCache.put(itemKey, priceData);

    // Log da atualização
    logger.info(String.format(
      "Preço atualizado para %s: base=%.2f, atual=%.2f, demanda=%d",
      itemKey,
      priceData.basePrice,
      priceData.currentPrice,
      priceData.demand
    ));
  }

  /**
   * Obtém o preço atual de um item
   */
  public double getCurrentPrice(ShopItem item) {
    if (!enabled) {
      return item.getBuyPrice();
    }

    String itemKey = item.getMaterial().name() + ":" + item.getData();
    PriceData priceData = priceCache.get(itemKey);

    if (priceData == null) {
      return item.getBuyPrice();
    }

    return priceData.currentPrice;
  }

  /**
   * Limpa preços antigos do cache
   */
  public void cleanup() {
    if (!enabled) {
      return;
    }

    long now = System.currentTimeMillis();
    priceCache.entrySet().removeIf(entry ->
      now - entry.getValue().lastUpdate > 86400000 // 24 horas
    );
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
              updatePrice(null, 0, false);
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
