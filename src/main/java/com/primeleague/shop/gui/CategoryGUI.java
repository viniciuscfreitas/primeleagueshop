package com.primeleague.shop.gui;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.events.CategoryOpenEvent;
import com.primeleague.shop.models.ShopCategory;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.ItemUtils;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cria e gerencia a GUI de uma categoria
 */
public class CategoryGUI {

  private final PrimeLeagueShopPlugin plugin;
  private final Map<UUID, PlayerCategoryData> playerData;
  private final int rows;
  private final String title;
  private List<ShopItem> items;

  // Constantes de navegação
  private static final int BACK_BUTTON_SLOT = 45;
  private static final int PREV_PAGE_SLOT = 48;
  private static final int NEXT_PAGE_SLOT = 50;

  // Constantes de layout
  private static final int FIRST_ITEM_SLOT = 0;
  private static final int ITEMS_PER_PAGE = 45;
  private static final int DEFAULT_ROWS = 6;

  // Constantes de mensagens de log
  private static final String LOG_CLICK_ERROR = "Erro ao processar clique na GUI da categoria: %s";
  private static final String LOG_OPENING_CATEGORY = "Abrindo categoria %s para jogador %s na página %d";
  private static final String LOG_CLICK_DEBUG = "Clique processado na categoria: slot=%d, item=%s";
  private static final String LOG_PLAYER_DATA = "Dados do jogador %s - Categoria: %s, Página: %s, Título: %s";

  /**
   * Classe para armazenar dados da categoria por jogador
   */
  private class PlayerCategoryData {
    private ShopCategory category;
    private int currentPage;
    private Map<Integer, ShopItem> itemSlotMap;
    private String inventoryTitle;

    public PlayerCategoryData() {
      this.currentPage = 0;
      this.itemSlotMap = new HashMap<>();
    }

    public void setData(ShopCategory category, int page, String title) {
      this.category = category;
      this.currentPage = page;
      this.inventoryTitle = title;
      this.itemSlotMap.clear();

      // Log para debug
      plugin.getLogger().info(String.format(LOG_PLAYER_DATA,
          "Player",
          category != null ? category.getName() : "null",
          String.valueOf(page),
          title));
    }
  }

  /**
   * Cria uma nova GUI de categoria
   *
   * @param plugin Instância do plugin
   */
  public CategoryGUI(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.playerData = new HashMap<>();
    this.rows = plugin.getConfig().getInt("gui.category.rows", 6);
    this.title = plugin.getConfigLoader().getMessage("gui.category.title", "Categoria");
  }

  /**
   * Obtém ou cria dados do jogador
   */
  private PlayerCategoryData getPlayerData(Player player) {
    return playerData.computeIfAbsent(player.getUniqueId(), k -> {
      plugin.getLogger().info("Criando novos dados para jogador " + player.getName());
      return new PlayerCategoryData();
    });
  }

  /**
   * Remove dados do jogador quando ele desconecta
   */
  public void removePlayerData(Player player) {
    playerData.remove(player.getUniqueId());
  }

  /**
   * Abre a GUI de uma categoria para um jogador
   *
   * @param player   Jogador
   * @param category Categoria a abrir
   * @param page     Página a mostrar
   */
  public void openCategoryGUI(Player player, ShopCategory category, int page) {
    plugin.getLogger().info(String.format(LOG_OPENING_CATEGORY, category.getName(), player.getName(), page));

    // Obtém o título já colorizado
    String title = plugin.getConfigLoader().getCategoryGuiTitle(category.getName());

    // Obtém ou cria os dados do jogador
    PlayerCategoryData data = getPlayerData(player);

    // Configura os dados antes de criar o inventário
    data.setData(category, page, title);

    plugin.getLogger().info("Título definido para jogador " + player.getName() + ": " + title);

    items = category.getItems();
    int startIndex = page * ITEMS_PER_PAGE;
    int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

    // Cria o inventário com o título já colorizado
    Inventory inventory = Bukkit.createInventory(null, rows * 9, title);

    if (plugin.getConfigLoader().shouldFillEmptySlots()) {
      ItemStack fillItem = ItemUtils.createItem(
          plugin.getConfigLoader().getFillMaterial(),
          plugin.getConfigLoader().getFillData(),
          " ");

      for (int i = 0; i < rows * 9; i++) {
        inventory.setItem(i, fillItem);
      }
    }

    for (int i = startIndex; i < endIndex; i++) {
      ShopItem item = items.get(i);
      int slot = FIRST_ITEM_SLOT + (i - startIndex);

      if (slot > FIRST_ITEM_SLOT + ITEMS_PER_PAGE - 1)
        break;

      ItemStack display = item.createDisplayItem(plugin.getConfigLoader().getCurrencySymbol());
      inventory.setItem(slot, display);
      data.itemSlotMap.put(slot, item);
    }

    setupNavigationButtons(inventory, data);

    player.openInventory(inventory);
  }

  private void setupNavigationButtons(Inventory inventory, PlayerCategoryData data) {
    // Botão voltar
    inventory.setItem(BACK_BUTTON_SLOT,
        createNavigationButton(Material.getMaterial(ShopConstants.MATERIAL_BACK_BUTTON), "§cVoltar"));

    // Botões de paginação
    if (data.currentPage > 0) {
      inventory.setItem(PREV_PAGE_SLOT,
          createNavigationButton(Material.getMaterial(ShopConstants.MATERIAL_PREVIOUS_PAGE), "§ePágina Anterior"));
    }

    if (hasNextPage(data)) {
      inventory.setItem(NEXT_PAGE_SLOT,
          createNavigationButton(Material.getMaterial(ShopConstants.MATERIAL_NEXT_PAGE), "§ePróxima Página"));
    }
  }

  private boolean hasNextPage(PlayerCategoryData data) {
    int startIndex = (data.currentPage + 1) * ITEMS_PER_PAGE;
    return startIndex < data.category.getItems().size();
  }

  /**
   * Processa um clique na GUI
   *
   * @param player       Jogador
   * @param slot         Slot clicado
   * @param isLeftClick  Se é um clique esquerdo
   * @param isShiftClick Se a tecla Shift está pressionada
   * @return true se o clique foi processado
   */
  public boolean handleClick(Player player, int slot, boolean isLeftClick, boolean isShiftClick) {
    try {
      PlayerCategoryData data = getPlayerData(player);

      // Validação inicial dos dados
      if (data == null) {
        plugin.getLogger().warning("Dados do jogador " + player.getName() + " são nulos");
        return false;
      }

      // Log detalhado do estado atual
      plugin.getLogger().info("Estado atual do jogador " + player.getName() + ":");
      plugin.getLogger().info("- Categoria: " + (data.category != null ? data.category.getName() : "null"));
      plugin.getLogger().info("- Página: " + data.currentPage);
      plugin.getLogger().info("- Título do inventário: " + data.inventoryTitle);
      plugin.getLogger().info("- Slot clicado: " + slot);

      String currentTitle = player.getOpenInventory().getTitle();

      // Validação do título do inventário
      if (data.inventoryTitle == null) {
        plugin.getLogger().warning("Título do inventário não foi definido para " + player.getName());
        return false;
      }

      if (!currentTitle.equals(data.inventoryTitle)) {
        plugin.getLogger().warning("Títulos não correspondem para " + player.getName());
        plugin.getLogger().warning("- Atual: '" + currentTitle + "'");
        plugin.getLogger().warning("- Esperado: '" + data.inventoryTitle + "'");
        return false;
      }

      // Validação da categoria
      if (data.category == null) {
        plugin.getLogger().warning("Categoria não definida para " + player.getName());
        return false;
      }

      // Log do processamento do clique
      plugin.getLogger().info(String.format("Processando clique para %s: slot=%d, categoria=%s, página=%d",
          player.getName(), slot, data.category.getName(), data.currentPage));

      // Processamento dos botões de navegação
      if (slot == BACK_BUTTON_SLOT) {
        plugin.getLogger().info("Voltando ao menu principal para " + player.getName());
        plugin.getShopGUI().openMainMenu(player);
        return true;
      }

      if (slot == PREV_PAGE_SLOT && data.currentPage > 0) {
        plugin.getLogger().info("Mudando para página anterior para " + player.getName());
        openCategoryGUI(player, data.category, data.currentPage - 1);
        return true;
      }

      if (slot == NEXT_PAGE_SLOT && hasNextPage(data)) {
        plugin.getLogger().info("Mudando para próxima página para " + player.getName());
        openCategoryGUI(player, data.category, data.currentPage + 1);
        return true;
      }

      // Processamento do clique em item
      ShopItem clickedItem = data.itemSlotMap.get(slot);
      if (clickedItem != null) {
        plugin.getLogger().info(String.format("Clique em item para %s: %s (slot %d)",
            player.getName(), clickedItem.getName(), slot));
        openConfirmationGUI(player, clickedItem, isLeftClick, isShiftClick);
        return true;
      }

      return false;

    } catch (Exception e) {
      plugin.getLogger().severe(String.format("Erro ao processar clique para %s: %s",
          player.getName(), e.getMessage()));
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Abre a GUI de confirmação para compra/venda
   *
   * @param player       Jogador
   * @param item         Item selecionado
   * @param isLeftClick  Se é clique esquerdo (compra) ou direito (venda)
   * @param isShiftClick Se Shift está pressionado (aumenta quantidade)
   */
  private void openConfirmationGUI(Player player, ShopItem item, boolean isLeftClick, boolean isShiftClick) {
    boolean isBuying = isLeftClick;
    int quantity = 1;
    if (isShiftClick) {
      quantity = isBuying ? plugin.getConfigLoader().getMaxBuyQuantity()
          : plugin.getConfigLoader().getMaxSellQuantity();
    }
    plugin.getConfirmationGUI().openConfirmationGUI(player, item, quantity, isBuying);
  }

  private ItemStack createNavigationButton(Material material, String name) {
    return ItemUtils.createItem(material, name, (List<String>) null);
  }
}
