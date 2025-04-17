package com.primeleague.shop.utils;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitários para manipulação de texto
 */
public class TextUtils {

  /**
   * Substitui códigos de cores (por exemplo, &a) por cores reais
   *
   * @param text Texto para colorir
   * @return Texto colorido
   */
  public static String colorize(String text) {
    return text != null ? ChatColor.translateAlternateColorCodes('&', text) : "";
  }

  /**
   * Remove todos os códigos de cores de uma string
   *
   * @param text Texto com cores
   * @return Texto sem cores
   */
  public static String stripColor(String text) {
    if (text == null)
      return "";
    return ChatColor.stripColor(text);
  }

  /**
   * Formata uma mensagem com variáveis
   *
   * @param message      Mensagem com placeholders
   * @param replacements Pares de (placeholder, valor)
   * @return Mensagem formatada
   */
  public static String format(String message, Object... replacements) {
    if (message == null)
      return "";
    if (replacements.length % 2 != 0) {
      throw new IllegalArgumentException("Número ímpar de argumentos de substituição");
    }

    String result = message;
    for (int i = 0; i < replacements.length; i += 2) {
      String placeholder = String.valueOf(replacements[i]);
      String replacement = String.valueOf(replacements[i + 1]);
      result = result.replace(placeholder, replacement);
    }

    return result;
  }

  /**
   * Formata um preço para exibição
   *
   * @param price          Preço
   * @param currencySymbol Símbolo da moeda
   * @return Preço formatado
   */
  public static String formatPrice(double price, String currencySymbol) {
    if (price == (int) price) {
      return String.format("%d%s", (int) price, currencySymbol);
    } else {
      return String.format("%.2f%s", price, currencySymbol);
    }
  }

  /**
   * Formata uma mensagem de compra
   *
   * @param message        Mensagem com placeholders
   * @param itemName       Nome do item
   * @param quantity       Quantidade
   * @param price          Preço total
   * @param currencySymbol Símbolo da moeda
   * @return Mensagem formatada
   */
  public static String formatBuyMessage(String message, String itemName, int quantity, double price,
      String currencySymbol) {
    return colorize(format(message,
        "{item}", itemName,
        "{quantity}", quantity,
        "{price}", formatPrice(price, currencySymbol),
        "{currency}", currencySymbol));
  }

  /**
   * Adiciona um prefixo a uma mensagem
   *
   * @param prefix  Prefixo
   * @param message Mensagem
   * @return Mensagem com prefixo
   */
  public static String prefix(String prefix, String message) {
    return colorize(prefix + message);
  }

  public static List<String> colorizeList(List<String> texts) {
    if (texts == null)
      return new ArrayList<>();

    List<String> colorized = new ArrayList<>();
    for (String text : texts) {
      colorized.add(colorize(text));
    }
    return colorized;
  }

  private TextUtils() {
    // Construtor privado para evitar instanciação
  }
}
