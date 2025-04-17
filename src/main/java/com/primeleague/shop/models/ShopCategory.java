package com.primeleague.shop.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma categoria da loja
 */
public class ShopCategory {

  private final String name;
  private final Material iconMaterial;
  private final byte iconData;
  private final int slot;
  private final String permission;
  private final List<ShopItem> items;

  /**
   * Cria uma nova categoria
   *
   * @param name         Nome da categoria
   * @param iconMaterial Material do ícone
   * @param iconData     Data value do ícone
   * @param slot         Slot na GUI principal
   * @param permission   Permissão necessária
   */
  public ShopCategory(String name, Material iconMaterial, byte iconData, int slot, String permission) {
    this.name = name;
    this.iconMaterial = iconMaterial;
    this.iconData = iconData;
    this.slot = slot;
    this.permission = permission;
    this.items = new ArrayList<>();
  }

  /**
   * Adiciona um item à categoria
   *
   * @param item Item a adicionar
   */
  public void addItem(ShopItem item) {
    items.add(item);
  }

  /**
   * Cria um ItemStack para exibição na GUI principal
   *
   * @return ItemStack configurado
   */
  public ItemStack createIcon() {
    ItemStack icon = new ItemStack(iconMaterial, 1, iconData);
    ItemMeta meta = icon.getItemMeta();

    meta.setDisplayName("§r" + name);

    List<String> lore = new ArrayList<>();
    lore.add("§7Clique para ver os itens");
    lore.add("§7Itens disponíveis: §e" + items.size());

    meta.setLore(lore);
    icon.setItemMeta(meta);

    return icon;
  }

  // Getters

  public String getName() {
    return name;
  }

  public Material getIconMaterial() {
    return iconMaterial;
  }

  public byte getIconData() {
    return iconData;
  }

  public int getSlot() {
    return slot;
  }

  public String getPermission() {
    return permission;
  }

  public List<ShopItem> getItems() {
    return items;
  }

  /**
   * Procura um item pelo nome
   *
   * @param itemName Nome do item a procurar
   * @return O item ou null se não encontrado
   */
  public ShopItem findItemByName(String itemName) {
    for (ShopItem item : items) {
      if (item.getName().equalsIgnoreCase(itemName)) {
        return item;
      }
    }
    return null;
  }

  /**
   * Procura um item que coincida com o ItemStack
   *
   * @param itemStack ItemStack a procurar
   * @return O item ou null se não encontrado
   */
  public ShopItem findItemByStack(ItemStack itemStack) {
    for (ShopItem item : items) {
      if (item.matches(itemStack)) {
        return item;
      }
    }
    return null;
  }

  public ItemStack getIcon() {
    return new ItemStack(iconMaterial, 1, iconData);
  }
}
