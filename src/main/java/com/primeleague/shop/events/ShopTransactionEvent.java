package com.primeleague.shop.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado após uma transação na loja ser concluída
 */
public class ShopTransactionEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  private final String playerName;
  private final String itemName;
  private final int quantity;
  private final double price;
  private final boolean isBuy;

  /**
   * Cria um novo evento de transação
   *
   * @param playerName Nome do jogador
   * @param itemName   Nome do item
   * @param quantity   Quantidade
   * @param price      Preço
   * @param isBuy      true se for compra, false se for venda
   */
  public ShopTransactionEvent(String playerName, String itemName, int quantity, double price, boolean isBuy) {
    this.playerName = playerName;
    this.itemName = itemName;
    this.quantity = quantity;
    this.price = price;
    this.isBuy = isBuy;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  /**
   * Obtém o nome do jogador que realizou a transação
   *
   * @return Nome do jogador
   */
  public String getPlayerName() {
    return playerName;
  }

  /**
   * Obtém o nome do item da transação
   *
   * @return Nome do item
   */
  public String getItemName() {
    return itemName;
  }

  /**
   * Obtém a quantidade da transação
   *
   * @return Quantidade
   */
  public int getQuantity() {
    return quantity;
  }

  /**
   * Obtém o preço total da transação
   *
   * @return Preço total
   */
  public double getPrice() {
    return price;
  }

  /**
   * Verifica se a transação é uma compra
   *
   * @return true se for compra
   */
  public boolean isBuy() {
    return isBuy;
  }

  /**
   * Verifica se a transação é uma venda
   *
   * @return true se for venda
   */
  public boolean isSell() {
    return !isBuy;
  }
}
