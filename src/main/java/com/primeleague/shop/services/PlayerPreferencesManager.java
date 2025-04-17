package com.primeleague.shop.services;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopItem;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

public class PlayerPreferencesManager {
  private final PrimeLeagueShopPlugin plugin;
  private final Map<String, PlayerPreferences> preferencesCache;
  private final File dataFile;

  private static class PlayerPreferences {
    private List<String> favoriteItems;
    private LastPurchase lastPurchase;
    private long lastAccess;

    public PlayerPreferences() {
      this.favoriteItems = new ArrayList<String>();
      this.lastPurchase = null;
      this.lastAccess = System.currentTimeMillis();
    }

    public long getLastAccess() {
      return lastAccess;
    }
  }

  private static class LastPurchase {
    private String itemId;
    private int quantity;
    private long timestamp;

    public LastPurchase(String itemId, int quantity) {
      this.itemId = itemId;
      this.quantity = quantity;
      this.timestamp = System.currentTimeMillis();
    }
  }

  public PlayerPreferencesManager(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.preferencesCache = new HashMap<String, PlayerPreferences>();
    this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
    loadData();
  }

  public boolean toggleFavorite(String playerName, ShopItem item) {
    PlayerPreferences prefs = getPlayerPreferences(playerName);
    String itemId = item.getMaterial().name() + ":" + item.getData();

    if (prefs.favoriteItems.contains(itemId)) {
      prefs.favoriteItems.remove(itemId);
      return false;
    } else {
      prefs.favoriteItems.add(itemId);
      return true;
    }
  }

  public boolean isFavorite(String playerName, ShopItem item) {
    PlayerPreferences prefs = getPlayerPreferences(playerName);
    String itemId = item.getMaterial().name() + ":" + item.getData();
    return prefs.favoriteItems.contains(itemId);
  }

  public List<String> getFavorites(String playerName) {
    PlayerPreferences prefs = getPlayerPreferences(playerName);
    return new ArrayList<String>(prefs.favoriteItems);
  }

  public void setLastPurchase(String playerName, ShopItem item, int quantity) {
    PlayerPreferences prefs = getPlayerPreferences(playerName);
    String itemId = item.getMaterial().name() + ":" + item.getData();
    prefs.lastPurchase = new LastPurchase(itemId, quantity);
  }

  public LastPurchase getLastPurchase(String playerName) {
    PlayerPreferences prefs = getPlayerPreferences(playerName);
    return prefs.lastPurchase;
  }

  private PlayerPreferences getPlayerPreferences(String playerName) {
    PlayerPreferences prefs = preferencesCache.get(playerName);
    if (prefs == null) {
      prefs = new PlayerPreferences();
      preferencesCache.put(playerName, prefs);
    }
    prefs.lastAccess = System.currentTimeMillis();
    return prefs;
  }

  private void loadData() {
    if (!dataFile.exists()) {
      return;
    }

    FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
    for (String playerName : config.getKeys(false)) {
      PlayerPreferences prefs = new PlayerPreferences();

      // Carrega favoritos
      prefs.favoriteItems = config.getStringList(playerName + ".favorites");

      // Carrega última compra
      if (config.contains(playerName + ".lastPurchase")) {
        String itemId = config.getString(playerName + ".lastPurchase.itemId");
        int quantity = config.getInt(playerName + ".lastPurchase.quantity");
        prefs.lastPurchase = new LastPurchase(itemId, quantity);
      }

      preferencesCache.put(playerName, prefs);
    }
  }

  public void saveAll() {
    FileConfiguration config = new YamlConfiguration();

    for (Map.Entry<String, PlayerPreferences> entry : preferencesCache.entrySet()) {
      String playerName = entry.getKey();
      PlayerPreferences prefs = entry.getValue();

      // Salva favoritos
      config.set(playerName + ".favorites", prefs.favoriteItems);

      // Salva última compra
      if (prefs.lastPurchase != null) {
        config.set(playerName + ".lastPurchase.itemId", prefs.lastPurchase.itemId);
        config.set(playerName + ".lastPurchase.quantity", prefs.lastPurchase.quantity);
      }
    }

    try {
      config.save(dataFile);
    } catch (Exception e) {
      plugin.getLogger().severe("Erro ao salvar preferências dos jogadores: " + e.getMessage());
    }
  }

  /**
   * Limpa os dados de cache de um jogador específico
   * 
   * @param playerName Nome do jogador
   */
  public void cleanupPlayerData(String playerName) {
    preferencesCache.remove(playerName);
  }

  /**
   * Executa limpeza geral dos caches
   */
  public void runCacheCleanup() {
    long now = System.currentTimeMillis();
    synchronized (preferencesCache) {
      preferencesCache.entrySet().removeIf(entry -> {
        PlayerPreferences prefs = entry.getValue();
        return now - prefs.getLastAccess() > 3600000; // 1 hora
      });
    }
  }
}
