package com.primeleague.shop.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilitários para manipulação de itens
 */
public class ItemUtils {

  /**
   * Obtém um Material pelo nome, compatível com 1.5.2
   *
   * @param name Nome do material
   * @return Material encontrado ou STONE se não encontrado
   */
  public static Material getMaterialByName(String name) {
    try {
      // Tenta converter para ID numérico primeiro
      if (name.matches("\\d+")) {
        int id = Integer.parseInt(name);
        return Material.getMaterial(id);
      }
      // Se não for número, tenta pelo nome
      return Material.valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) {
      return Material.STONE;
    }
  }

  /**
   * Cria um ItemStack com nome e lore
   *
   * @param material Material do item
   * @param data     Data value do item
   * @param amount   Quantidade do item
   * @param name     Nome do item (pode ser null)
   * @param lore     Descrição do item (pode ser null)
   * @return ItemStack configurado
   */
  public static ItemStack createItem(Material material, byte data, int amount, String name, List<String> lore) {
    ItemStack item = new ItemStack(material, amount, data);
    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      if (name != null && !name.isEmpty()) {
        meta.setDisplayName(TextUtils.colorize(name));
      }

      if (lore != null && !lore.isEmpty()) {
        meta.setLore(TextUtils.colorizeList(lore));
      }

      item.setItemMeta(meta);
    }

    return item;
  }

  /**
   * Cria um ItemStack com nome
   */
  public static ItemStack createItem(Material material, byte data, String name) {
    return createItem(material, data, 1, name, null);
  }

  /**
   * Cria um ItemStack com nome e lore em array
   */
  public static ItemStack createItem(Material material, String name, String[] lore) {
    return createItem(material, (byte) 0, 1, name, lore != null ? Arrays.asList(lore) : null);
  }

  /**
   * Verifica se dois ItemStacks são similares (mesmo tipo e data)
   *
   * @param item1 Primeiro item
   * @param item2 Segundo item
   * @return true se forem similares
   */
  public static boolean isSimilar(ItemStack item1, ItemStack item2) {
    if (item1 == null || item2 == null)
      return false;

    return item1.getType() == item2.getType() &&
        item1.getData().getData() == item2.getData().getData();
  }

  /**
   * Converte um nome de material para um Material
   * Compatível com 1.5.2, usando nomes antigos
   *
   * @param materialName Nome do material
   * @return Material ou STONE se não encontrado
   */
  public static Material parseMaterial(String materialName) {
    if (materialName == null || materialName.isEmpty()) {
      return Material.STONE;
    }

    // Tentando obter diretamente
    Material material = Material.getMaterial(materialName);
    if (material != null) {
      return material;
    }

    // Convertendo nomes comuns
    materialName = materialName.toUpperCase().replace(" ", "_");

    // Tentando novamente
    material = Material.getMaterial(materialName);
    return material != null ? material : Material.STONE;
  }

  public static ItemStack createItem(Material material, String name, List<String> lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();

    if (name != null) {
      meta.setDisplayName(TextUtils.colorize(name));
    }

    if (lore != null) {
      meta.setLore(TextUtils.colorizeList(lore));
    }

    item.setItemMeta(meta);
    return item;
  }

  /**
   * Atualiza o lore de um ItemStack
   */
  public static void updateLore(ItemStack item, List<String> lore) {
    if (item == null) return;

    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setLore(lore != null ? TextUtils.colorizeList(lore) : null);
      item.setItemMeta(meta);
    }
  }

  private ItemUtils() {
    // Construtor privado para evitar instanciação
  }
}
