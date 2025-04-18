package com.primeleague.shop.gui;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import com.primeleague.shop.models.ShopCategory;
import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.utils.ItemUtils;
import com.primeleague.shop.utils.ShopConstants;
import com.primeleague.shop.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.Material;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cria e gerencia a GUI principal da loja
 */
public class ShopGUI {

  private final String title = "Loja Prime";
  private final PrimeLeagueShopPlugin plugin;
  private final Map<Integer, ShopCategory> slotMap;
  private final Map<UUID, String> searchQuery;
  private final Map<UUID, List<String>> favorites;

  // Slots para elementos especiais
  private static final int SEARCH_SLOT = 4;
  private static final int BALANCE_SLOT = 49;
  private static final int CART_SLOT = 46;
  private static final int HISTORY_SLOT = 47;
  private static final int FAVORITES_SLOT = 48;

  // Constantes de log
  private static final String LOG_CLICK = "Menu principal: Clique no slot %d, categoria=%s";
  private static final String LOG_OPEN = "Menu principal: Abrindo para %s com %d categorias";

  /**
   * Cria uma nova GUI principal
   *
   * @param plugin Instância do plugin
   */
  public ShopGUI(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.slotMap = new HashMap<>();
    this.searchQuery = new HashMap<>();
    this.favorites = new HashMap<>();
  }

  /**
   * Abre o menu principal da loja para um jogador
   *
   * @param player Jogador
   */
  public void openMainMenu(Player player) {
    slotMap.clear();

    String title = plugin.getConfigLoader().getMessage("gui.main_shop_title", "&8Loja Prime League");
    Inventory inv = Bukkit.createInventory(null, 54, TextUtils.colorize(title));

    // Adiciona barra de pesquisa
    ItemStack searchBar = createSearchBar(player);
    inv.setItem(SEARCH_SLOT, searchBar);

    // Adiciona saldo do jogador
    ItemStack balance = createBalanceItem(player);
    inv.setItem(BALANCE_SLOT, balance);

    // Adiciona botão do carrinho
    ItemStack cart = createCartButton(player);
    inv.setItem(CART_SLOT, cart);

    // Adiciona botão de histórico
    ItemStack history = createHistoryButton();
    inv.setItem(HISTORY_SLOT, history);

    // Adiciona botão de favoritos
    ItemStack favoritesButton = createFavoritesButton(player);
    inv.setItem(FAVORITES_SLOT, favoritesButton);

    // Preenche slots vazios com vidro decorativo
    ItemStack filler = createFillerGlass();
    for (int i = 0; i < inv.getSize(); i++) {
      if (inv.getItem(i) == null) {
        inv.setItem(i, filler);
      }
    }

    List<ShopCategory> categories = plugin.getShopManager().getCategories();

    // Organiza as categorias em slots específicos
    Map<String, Integer> categorySlots = new HashMap<>();
    categorySlots.put("Combate", 10);      // Primeira linha - Itens de combate
    categorySlots.put("Blocos", 11);       // Blocos de construção
    categorySlots.put("Minérios", 12);     // Minérios e seus blocos
    categorySlots.put("Ferramentas", 13);  // Todas as ferramentas
    categorySlots.put("Alimentos", 14);    // Comidas e alimentos
    categorySlots.put("Poções", 15);       // Todas as poções
    categorySlots.put("Ovos", 16);         // Ovos de spawn
    categorySlots.put("Livros", 22);       // Livros encantados (centro da segunda linha)

    // Adiciona as categorias nos slots definidos
    for (ShopCategory category : categories) {
      Integer slot = categorySlots.get(category.getName());
      if (slot != null) {
        ItemStack icon = category.getIcon();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(TextUtils.colorize("&a" + category.getName()));

        List<String> lore = new ArrayList<>();
        lore.add(TextUtils.colorize("&7Clique para ver os itens"));
        lore.add(TextUtils.colorize("&7desta categoria"));
        lore.add("");

        // Adiciona descrição específica para cada categoria
        switch(category.getName()) {
          case "Combate":
            lore.add(TextUtils.colorize("&7• Espadas, Arcos, Armaduras"));
            lore.add(TextUtils.colorize("&7• Escudos, Flechas"));
            break;
          case "Blocos":
            lore.add(TextUtils.colorize("&7• Blocos de Construção"));
            lore.add(TextUtils.colorize("&7• Decorativos"));
            break;
          case "Minérios":
            lore.add(TextUtils.colorize("&7• Minérios Brutos"));
            lore.add(TextUtils.colorize("&7• Blocos de Minérios"));
            break;
          case "Ferramentas":
            lore.add(TextUtils.colorize("&7• Picaretas, Pás, Machados"));
            lore.add(TextUtils.colorize("&7• Enxadas, Tesouras"));
            break;
          case "Alimentos":
            lore.add(TextUtils.colorize("&7• Carnes, Vegetais"));
            lore.add(TextUtils.colorize("&7• Alimentos Cozidos"));
            break;
          case "Poções":
            lore.add(TextUtils.colorize("&7• Poções de Efeito"));
            lore.add(TextUtils.colorize("&7• Ingredientes"));
            break;
          case "Ovos":
            lore.add(TextUtils.colorize("&7• Ovos de Spawn"));
            lore.add(TextUtils.colorize("&7• Geradores"));
            break;
          case "Livros":
            lore.add(TextUtils.colorize("&7• Livros Encantados"));
            lore.add(TextUtils.colorize("&7• Encantamentos"));
            break;
        }

        lore.add("");
        lore.add(TextUtils.colorize("&7Total de itens: &f" + category.getItems().size()));
        meta.setLore(lore);

        icon.setItemMeta(meta);
        inv.setItem(slot, icon);
        slotMap.put(slot, category);
      }
    }

    player.openInventory(inv);
    String message = plugin.getConfigLoader().getMessage("shop_opened", "&aVocê abriu a loja!");
    player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() + message));
  }

  private ItemStack createSearchBar(Player player) {
    ItemStack search = new ItemStack(Material.HOPPER);
    ItemMeta meta = search.getItemMeta();
    meta.setDisplayName(TextUtils.colorize("&ePesquisar Itens"));

    List<String> lore = new ArrayList<>();
    String currentSearch = searchQuery.get(player.getUniqueId());
    if (currentSearch != null && !currentSearch.isEmpty()) {
      lore.add(TextUtils.colorize("&7Pesquisa atual: &f" + currentSearch));
    }
    lore.add(TextUtils.colorize("&7Clique para pesquisar"));
    meta.setLore(lore);
    search.setItemMeta(meta);
    return search;
  }

  private ItemStack createBalanceItem(Player player) {
    ItemStack balance = new ItemStack(Material.EMERALD);
    ItemMeta meta = balance.getItemMeta();
    meta.setDisplayName(TextUtils.colorize("&aSeu Saldo"));

    List<String> lore = new ArrayList<>();
    double playerBalance = plugin.getEconomy().getBalance(player.getName());
    lore.add(TextUtils.colorize("&7Saldo: &a$" + String.format("%.2f", playerBalance)));
    meta.setLore(lore);
    balance.setItemMeta(meta);
    return balance;
  }

  private ItemStack createCartButton(Player player) {
    ItemStack cart = new ItemStack(Material.CHEST);
    ItemMeta meta = cart.getItemMeta();
    meta.setDisplayName(TextUtils.colorize("&6Carrinho"));

    List<String> lore = new ArrayList<>();
    lore.add(TextUtils.colorize("&7Clique para ver seu carrinho"));
    meta.setLore(lore);
    cart.setItemMeta(meta);
    return cart;
  }

  private ItemStack createHistoryButton() {
    ItemStack history = new ItemStack(Material.BOOK);
    ItemMeta meta = history.getItemMeta();
    meta.setDisplayName(TextUtils.colorize("&eHistórico"));

    List<String> lore = new ArrayList<>();
    lore.add(TextUtils.colorize("&7Veja suas últimas transações"));
    meta.setLore(lore);
    history.setItemMeta(meta);
    return history;
  }

  private ItemStack createFavoritesButton(Player player) {
    ItemStack favorites = new ItemStack(Material.NETHER_STAR);
    ItemMeta meta = favorites.getItemMeta();
    meta.setDisplayName(TextUtils.colorize("&bFavoritos"));

    List<String> lore = new ArrayList<>();
    List<String> playerFavorites = this.favorites.get(player.getUniqueId());
    if (playerFavorites != null && !playerFavorites.isEmpty()) {
      lore.add(TextUtils.colorize("&7Você tem &b" + playerFavorites.size() + "&7 itens favoritos"));
    } else {
      lore.add(TextUtils.colorize("&7Clique com SHIFT em um item"));
      lore.add(TextUtils.colorize("&7para adicionar aos favoritos"));
    }
    meta.setLore(lore);
    favorites.setItemMeta(meta);
    return favorites;
  }

  private ItemStack createFillerGlass() {
    ItemStack glass = new ItemStack(Material.THIN_GLASS, 1);
    ItemMeta meta = glass.getItemMeta();
    meta.setDisplayName(" ");
    glass.setItemMeta(meta);
    return glass;
  }

  /**
   * Processa um clique em um slot
   *
   * @param player Jogador
   * @param slot   Slot clicado
   * @return true se o slot continha uma categoria
   */
  public boolean handleClick(Player player, int slot) {
    // Verifica se o slot contém uma categoria
    if (slotMap.containsKey(slot)) {
      ShopCategory category = slotMap.get(slot);

      // Verifica permissão
      if (!category.getPermission().isEmpty() && !player.hasPermission(category.getPermission())) {
        player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() +
            plugin.getConfigLoader().getMessage("no_permission", "&cVocê não tem permissão para isso.")));
        return true;
      }

      // Abre a categoria usando a instância compartilhada
      plugin.getCategoryGUI().openCategoryGUI(player, category, 0);

      // Mensagem
      String message = plugin.getConfigLoader()
          .getMessage("category_opened", "&aVocê abriu a categoria &e{category}&a!")
          .replace("{category}", category.getName());
      player.sendMessage(TextUtils.colorize(plugin.getConfigLoader().getPrefix() + message));

      return true;
    }

    return false;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    if (!event.getView().getTitle().equals(title)) return;

    event.setCancelled(true);
    Player player = (Player) event.getWhoClicked();
    ItemStack clickedItem = event.getCurrentItem();
    int slot = event.getSlot();

    if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

    // Gerencia cliques nos botões especiais
    if (slot == SEARCH_SLOT) {
      handleSearchClick(player);
      return;
    }

    if (slot == CART_SLOT) {
      openCart(player);
      return;
    }

    if (slot == HISTORY_SLOT) {
      openHistory(player);
      return;
    }

    if (slot == FAVORITES_SLOT) {
      openFavorites(player);
      return;
    }

    // Gerencia cliques em itens normais
    if (event.getClick() == ClickType.SHIFT_LEFT) {
      toggleFavorite(player, clickedItem);
      return;
    }

    // ... existing click handling code ...
  }

  private void handleSearchClick(Player player) {
    player.closeInventory();
    player.sendMessage(TextUtils.colorize("&ePor favor, digite o termo de pesquisa no chat (ou 'cancelar' para sair):"));

    // Registra o jogador em modo de pesquisa
    plugin.getChatInputManager().awaitChatInput(player, (input) -> {
      if (input.equalsIgnoreCase("cancelar")) {
        player.sendMessage(TextUtils.colorize("&cPesquisa cancelada."));
        openMainMenu(player);
        return;
      }

      searchQuery.put(player.getUniqueId(), input);
      openSearchResults(player, input);
    });
  }

  private void openSearchResults(Player player, String query) {
    Inventory inv = Bukkit.createInventory(null, 54, TextUtils.colorize("&8Resultados da Pesquisa"));

    List<ShopItem> results = new ArrayList<>();
    for (ShopCategory category : plugin.getShopManager().getCategories()) {
      for (ShopItem item : category.getItems()) {
        if (item.getName().toLowerCase().contains(query.toLowerCase())) {
          results.add(item);
        }
      }
    }

    // Adiciona barra de pesquisa com termo atual
    inv.setItem(SEARCH_SLOT, createSearchBar(player));

    // Adiciona resultados
    int slot = 10;
    for (ShopItem item : results) {
      if (slot > 43) break;
      inv.setItem(slot, item.createDisplayItem(plugin.getConfigLoader().getCurrencySymbol()));
      slot += (slot % 9 == 7) ? 3 : 1;
    }

    // Adiciona botão de voltar
    ItemStack back = new ItemStack(Material.ARROW);
    ItemMeta meta = back.getItemMeta();
    meta.setDisplayName(TextUtils.colorize("&cVoltar"));
    back.setItemMeta(meta);
    inv.setItem(49, back);

    player.openInventory(inv);
  }

  private void toggleFavorite(Player player, ItemStack item) {
    String itemId = getItemId(item);
    if (itemId == null) return;

    List<String> playerFavorites = favorites.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

    if (playerFavorites.contains(itemId)) {
      playerFavorites.remove(itemId);
      player.sendMessage(TextUtils.colorize("&cItem removido dos favoritos!"));
    } else {
      playerFavorites.add(itemId);
      player.sendMessage(TextUtils.colorize("&aItem adicionado aos favoritos!"));
    }

    // Atualiza o botão de favoritos
    player.getOpenInventory().getTopInventory().setItem(FAVORITES_SLOT, createFavoritesButton(player));
  }

  private void openFavorites(Player player) {
    Inventory inv = Bukkit.createInventory(null, 54, TextUtils.colorize("&8Seus Favoritos"));
    List<String> playerFavorites = favorites.get(player.getUniqueId());

    if (playerFavorites == null || playerFavorites.isEmpty()) {
      player.sendMessage(TextUtils.colorize("&cVocê não tem itens favoritos!"));
      return;
    }

    int slot = 10;
    for (String itemId : playerFavorites) {
      ShopItem item = plugin.getShopManager().getItemById(itemId);
      if (item != null) {
        inv.setItem(slot, item.createDisplayItem(plugin.getConfigLoader().getCurrencySymbol()));
        slot += (slot % 9 == 7) ? 3 : 1;
      }
    }

    // Adiciona botão de voltar
    ItemStack back = new ItemStack(Material.ARROW);
    ItemMeta meta = back.getItemMeta();
    meta.setDisplayName(TextUtils.colorize("&cVoltar"));
    back.setItemMeta(meta);
    inv.setItem(49, back);

    player.openInventory(inv);
  }

  private void openCart(Player player) {
    // Implementação do carrinho será feita em uma classe separada
    plugin.getCartManager().openCart(player);
  }

  private void openHistory(Player player) {
    // Implementação do histórico será feita em uma classe separada
    plugin.getTransactionHistory().openHistory(player);
  }

  private String getItemId(ItemStack item) {
    if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;

    List<String> lore = item.getItemMeta().getLore();
    for (String line : lore) {
      if (line.startsWith("ID: ")) {
        return line.substring(4);
      }
    }
    return null;
  }

  private double getItemPrice(String itemId) {
    // Implementar lógica para obter o preço do item do config.yml
    return plugin.getConfig().getDouble("items." + itemId + ".price", 0.0);
  }
}
