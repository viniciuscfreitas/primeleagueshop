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
   * @param pricePrefix Prefixo do preço
   * @return ItemStack configurado
   */
  public ItemStack createDisplayItem(String pricePrefix) {
    ItemStack item = new ItemStack(material, 1, data);
    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      meta.setDisplayName(TextUtils.colorize("§f" + displayName));

      List<String> displayLore = new ArrayList<>(description);
      displayLore.addAll(lore);

      if (buyPrice > 0) {
        displayLore.add(pricePrefix + String.format("%.2f", buyPrice));
      }
      if (sellPrice > 0) {
        displayLore.add("§7Venda: §e$" + String.format("%.2f", sellPrice));
      }

      meta.setLore(TextUtils.colorizeList(displayLore));
      item.setItemMeta(meta);
    }

    return item;
  }

  /**
   * Cria um ItemStack real para dar ao jogador
   *
   * @param quantidade Quantidade do item
   * @return ItemStack para o jogador
   */
  public ItemStack createActualItem(int quantidade) {
    ItemStack item = new ItemStack(material, quantidade, data);
    return item;
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
