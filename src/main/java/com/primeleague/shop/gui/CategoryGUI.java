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
  private final Map<UUID, PreviewData> previewData;
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

  private static final int PREVIEW_SLOT = 49;
  private static final int BACK_SLOT = 45;
  private static final int NEXT_SLOT = 53;
  private static final int FAVORITE_SLOT = 48;

  private static class PlayerCategoryData {
    private ShopCategory currentCategory;
    private int currentPage;
    private final Map<Integer, ShopItem> slotMap;

    public PlayerCategoryData() {
      this.slotMap = new HashMap<>();
      this.currentPage = 1;
    }
  }

  private static class PreviewData {
    private final ShopItem item;
    private final int quantity;
    private final long timestamp;

    public PreviewData(ShopItem item, int quantity) {
      this.item = item;
      this.quantity = quantity;
      this.timestamp = System.currentTimeMillis();
    }

    public boolean isExpired() {
      return System.currentTimeMillis() - timestamp > 300000; // 5 minutos
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
    this.previewData = new HashMap<>();
    this.rows = plugin.getConfig().getInt("gui.category.rows", 6);
    this.title = plugin.getConfigLoader().getMessage("gui.category.title", "Categoria");
  }

  /**
   * Obtém ou cria dados do jogador
   */
  private PlayerCategoryData getPlayerData(Player player) {
    return playerData.computeIfAbsent(player.getUniqueId(), k -> new PlayerCategoryData());
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
    data.currentCategory = category;
    data.currentPage = page;
    data.slotMap.clear();

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

    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();
    for (int i = startIndex; i < endIndex; i++) {
      ShopItem item = items.get(i);
      int slot = FIRST_ITEM_SLOT + (i - startIndex);

      if (slot > FIRST_ITEM_SLOT + ITEMS_PER_PAGE - 1)
        break;

      ItemStack display = item.createDisplayItem(currencySymbol);
      inventory.setItem(slot, display);
      data.slotMap.put(slot, item);
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
    return startIndex < data.currentCategory.getItems().size();
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

      if (data == null) {
        plugin.getLogger().warning("Dados do jogador " + player.getName() + " são nulos");
        return false;
      }

      plugin.getLogger().info(String.format("Processando clique para %s: slot=%d, categoria=%s, página=%d",
          player.getName(), slot, data.currentCategory.getName(), data.currentPage));

      if (slot == BACK_BUTTON_SLOT) {
        plugin.getLogger().info("Voltando ao menu principal para " + player.getName());
        plugin.getShopGUI().openMainMenu(player);
        return true;
      }

      if (slot == PREV_PAGE_SLOT && data.currentPage > 0) {
        plugin.getLogger().info("Mudando para página anterior para " + player.getName());
        openCategoryGUI(player, data.currentCategory, data.currentPage - 1);
        return true;
      }

      if (slot == NEXT_PAGE_SLOT && hasNextPage(data)) {
        plugin.getLogger().info("Mudando para próxima página para " + player.getName());
        openCategoryGUI(player, data.currentCategory, data.currentPage + 1);
        return true;
      }

      ShopItem clickedItem = data.slotMap.get(slot);
      if (clickedItem != null) {
        plugin.getLogger().info(String.format("Clique em item para %s: %s (slot %d)",
            player.getName(), clickedItem.getName(), slot));

        if (isShiftClick && !isLeftClick) {
          showPreview(player, clickedItem);
        } else {
          plugin.getConfirmationGUI().openBuyConfirmation(player, clickedItem, isLeftClick);
        }
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

  private ItemStack createNavigationButton(Material material, String name) {
    return ItemUtils.createItem(material, name, (List<String>) null);
  }

  public void openCategory(Player player, ShopCategory category, int page) {
    PlayerCategoryData data = getPlayerData(player);
    data.currentCategory = category;
    data.currentPage = page;

    String title = TextUtils.colorize(plugin.getConfigLoader().getMessage("gui.category_title",
        "&8{category}").replace("{category}", category.getName()));
    Inventory inv = Bukkit.createInventory(null, rows * 9, title);

    // Preenche slots vazios
    if (plugin.getConfigLoader().shouldFillEmptySlots()) {
        ItemStack filler = new ItemStack(
            plugin.getConfigLoader().getFillMaterial(),
            1,
            (short) plugin.getConfigLoader().getFillData());
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    List<ShopItem> items = category.getItems();
    int totalPages = (items.size() - 1) / 28 + 1;
    int startIndex = (page - 1) * 28;
    int endIndex = Math.min(startIndex + 28, items.size());

    // Adiciona itens
    int slot = 10;
    String currencySymbol = plugin.getConfigLoader().getCurrencySymbol();
    for (int i = startIndex; i < endIndex; i++) {
        ShopItem item = items.get(i);
        ItemStack icon = item.createDisplayItem(currencySymbol);
        inv.setItem(slot, icon);
        data.slotMap.put(slot, item);

        slot += (slot % 9 == 7) ? 3 : 1;
    }

    // Adiciona botão de voltar
    ItemStack backIcon = new ItemStack(Material.ARROW);
    ItemMeta backMeta = backIcon.getItemMeta();
    if (backMeta != null) {
        backMeta.setDisplayName(TextUtils.colorize("&cVoltar"));
        backIcon.setItemMeta(backMeta);
    }
    inv.setItem(BACK_SLOT, backIcon);

    // Adiciona botões de navegação se necessário
    if (page > 1) {
        ItemStack prevIcon = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevIcon.getItemMeta();
        if (prevMeta != null) {
            prevMeta.setDisplayName(TextUtils.colorize("&ePágina anterior"));
            prevIcon.setItemMeta(prevMeta);
        }
        inv.setItem(NEXT_SLOT - 1, prevIcon);
    }

    if (page < totalPages) {
        ItemStack nextIcon = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextIcon.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName(TextUtils.colorize("&ePróxima página"));
            nextIcon.setItemMeta(nextMeta);
        }
        inv.setItem(NEXT_SLOT, nextIcon);
    }

    // Adiciona botão de preview
    ItemStack previewIcon = new ItemStack(Material.GLASS);
    ItemMeta previewMeta = previewIcon.getItemMeta();
    if (previewMeta != null) {
        previewMeta.setDisplayName(TextUtils.colorize("&bPreview de Itens"));
        List<String> previewLore = new ArrayList<>();
        previewLore.add(TextUtils.colorize("&7Clique em um item com"));
        previewLore.add(TextUtils.colorize("&7botão direito para ver"));
        previewLore.add(TextUtils.colorize("&7como ele ficará no seu"));
        previewLore.add(TextUtils.colorize("&7inventário"));
        previewMeta.setLore(previewLore);
        previewIcon.setItemMeta(previewMeta);
    }
    inv.setItem(PREVIEW_SLOT, previewIcon);

    player.openInventory(inv);
  }

  private void showPreview(Player player, ShopItem item) {
    // Salva inventário atual
    ItemStack[] oldContents = player.getInventory().getContents().clone();

    // Limpa inventário
    player.getInventory().clear();

    // Adiciona o item para preview
    player.getInventory().addItem(item.toItemStack(64)); // Usa quantidade máxima padrão do Minecraft

    // Salva dados do preview
    previewData.put(player.getUniqueId(), new PreviewData(item, 64));

    // Agenda restauração do inventário
    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
      @Override
      public void run() {
        if (player.isOnline()) {
          PreviewData preview = previewData.get(player.getUniqueId());
          if (preview != null && !preview.isExpired()) {
            player.getInventory().setContents(oldContents);
            player.sendMessage(TextUtils.colorize("&aPreview finalizado!"));
            previewData.remove(player.getUniqueId());
          }
        }
      }
    }, 100L); // 5 segundos

    player.sendMessage(TextUtils.colorize("&aPreview ativo por 5 segundos!"));
  }

  private int getTotalPages(ShopCategory category) {
    return (category.getItems().size() - 1) / 28 + 1;
  }

  public void cleanup() {
    // Remove dados de preview expirados
    previewData.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }
}
