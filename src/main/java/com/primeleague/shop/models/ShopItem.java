package com.primeleague.shop.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.primeleague.shop.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um item disponível na loja
 */
public class ShopItem {

  private final Material material;
  private final byte data;
  private final String displayName;
  private final List<String> description;
  private final double buyPrice;
  private final double sellPrice;
  private final String permission;
  private final List<String> lore;
  private final ShopCategory category;

  /**
   * Cria um novo item da loja
   *
   * @param material    Material do item
   * @param data        Data value do item
   * @param displayName Nome do item
   * @param description Descrição do item
   * @param buyPrice    Preço de compra
   * @param sellPrice   Preço de venda
   * @param permission  Permissão necessária para comprar (pode ser vazia)
   * @param lore        Descrição do item
   * @param category    Categoria a qual o item pertence
   */
  public ShopItem(Material material, byte data, String displayName, List<String> description, double buyPrice,
      double sellPrice, String permission, List<String> lore, ShopCategory category) {
    this.material = material;
    this.data = data;
    this.displayName = displayName;
    this.description = description != null ? description : new ArrayList<>();
    this.buyPrice = buyPrice;
    this.sellPrice = sellPrice;
    this.permission = permission;
    this.lore = lore != null ? lore : new ArrayList<>();
    this.category = category;
  }

  /**
   * Cria um ItemStack para exibição na GUI da loja
   *
   * @param currencySymbol Símbolo da moeda
   * @return ItemStack configurado
   */
  public ItemStack createDisplayItem(String currencySymbol) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(TextUtils.colorize("&f" + displayName));
    List<String> lore = new ArrayList<>();

    // Adiciona descrição se existir
    if (description != null && !description.isEmpty()) {
        lore.addAll(description);
        lore.add("");
    }

    // Adiciona preços
    if (buyPrice > 0) {
        lore.add(TextUtils.colorize("&aComprar por: " + currencySymbol + String.format("%.2f", buyPrice)));
    }
    if (sellPrice > 0) {
        lore.add(TextUtils.colorize("&cVender por: " + currencySymbol + String.format("%.2f", sellPrice)));
    }

    // Adiciona instruções
    lore.add("");
    if (buyPrice > 0) {
        lore.add(TextUtils.colorize("&7Botão esquerdo para comprar"));
    }
    if (sellPrice > 0) {
        lore.add(TextUtils.colorize("&7Botão direito para vender"));
    }

    meta.setLore(lore);
    item.setItemMeta(meta);
    return item;
  }

  /**
   * Verifica se o item pode ser empilhado
   * @return true se o item pode ser empilhado, false caso contrário
   */
  private boolean isStackable() {
    return !(material.name().contains("SWORD") ||
           material.name().contains("HELMET") ||
           material.name().contains("CHESTPLATE") ||
           material.name().contains("LEGGINGS") ||
           material.name().contains("BOOTS") ||
           material.name().contains("BOW") ||
           material.name().contains("SHIELD") ||
           material.name().contains("ELYTRA") ||
           material.name().contains("TRIDENT"));
  }

  /**
   * Cria um ItemStack real para dar ao jogador
   * @param quantidade Quantidade do item
   * @return Lista de ItemStacks para o jogador
   */
  public List<ItemStack> createActualItems(int quantidade) {
    List<ItemStack> items = new ArrayList<>();

    if (isStackable()) {
        // Se for empilhável, cria um único stack
        items.add(new ItemStack(material, quantidade, data));
    } else {
        // Se não for empilhável, cria items individuais
        for (int i = 0; i < quantidade; i++) {
            items.add(new ItemStack(material, 1, data));
        }
    }

    return items;
  }

  /**
   * Calcula o preço total para uma quantidade de itens
   *
   * @param quantidade Quantidade de itens
   * @param isBuying   Se é uma compra (true) ou venda (false)
   * @return Preço total
   */
  public double calculatePrice(int quantidade, boolean isBuying) {
    return isBuying ? buyPrice * quantidade : sellPrice * quantidade;
  }

  /**
   * Retorna o nome do item
   *
   * @return Nome do item
   */
  public String getName() {
    return displayName;
  }

  /**
   * Cria um ItemStack deste item da loja
   */
  public ItemStack toItemStack(int amount) {
    ItemStack item = new ItemStack(material, amount, (short) data);
    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      meta.setDisplayName(TextUtils.colorize("&r" + displayName));

      if (lore != null && !lore.isEmpty()) {
        List<String> colorizedLore = new ArrayList<>();
        for (String line : lore) {
          colorizedLore.add(TextUtils.colorize(line));
        }
        meta.setLore(colorizedLore);
      }

      item.setItemMeta(meta);
    }

    return item;
  }

  // Getters

  public Material getMaterial() {
    return material;
  }

  public byte getData() {
    return data;
  }

  public String getDisplayName() {
    return displayName;
  }

  public List<String> getDescription() {
    return new ArrayList<>(description);
  }

  public double getBuyPrice() {
    return buyPrice;
  }

  public double getSellPrice() {
    return sellPrice;
  }

  public String getPermission() {
    return permission;
  }

  public List<String> getLore() {
    return new ArrayList<>(lore);
  }

  public ShopCategory getCategory() {
    return category;
  }

  /**
   * Verifica se o ItemStack corresponde a este ShopItem
   *
   * @param itemStack ItemStack a verificar
   * @return true se o ItemStack corresponde a este ShopItem
   */
  public boolean matches(ItemStack itemStack) {
    // Evita NPE
    if (itemStack == null) {
      return false;
    }

    // Verifica só o material e data
    if (itemStack.getType() == material &&
        itemStack.getData().getData() == data) {
      return true;
    }

    // Verifica pelo nome também
    if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
      String displayName = itemStack.getItemMeta().getDisplayName();
      if (displayName != null && displayName.contains(this.displayName)) {
        return true;
      }
    }

    return false;
  }
}
