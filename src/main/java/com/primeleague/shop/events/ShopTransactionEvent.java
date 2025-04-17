package com.primeleague.shop.events;

import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.models.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado após uma transação na loja ser concluída
 */
public class ShopTransactionEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  private final Player player;
  private final ShopItem item;
  private final int quantity;
  private final double totalPrice;
  private final Transaction.TransactionType type;

  /**
   * Cria um novo evento de transação
   *
   * @param player     Jogador
   * @param item       Item da transação
   * @param quantity   Quantidade
   * @param totalPrice Preço total
   * @param type       Tipo de transação
   */
  public ShopTransactionEvent(Player player, ShopItem item, int quantity, double totalPrice,
      Transaction.TransactionType type) {
    this.player = player;
    this.item = item;
    this.quantity = quantity;
    this.totalPrice = totalPrice;
    this.type = type;
  }

  /**
   * Cria um novo evento de transação a partir de um objeto Transaction
   *
   * @param player      Jogador
   * @param transaction Transação
   */
  public ShopTransactionEvent(Player player, Transaction transaction) {
    this.player = player;
    this.item = transaction.getItem();
    this.quantity = transaction.getQuantity();
    this.totalPrice = transaction.getTotalPrice();
    this.type = transaction.getType();
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  /**
   * Obtém o jogador que realizou a transação
   *
   * @return Jogador
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Obtém o item da transação
   *
   * @return Item
   */
  public ShopItem getItem() {
    return item;
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
  public double getTotalPrice() {
    return totalPrice;
  }

  /**
   * Obtém o tipo da transação
   *
   * @return Tipo da transação
   */
  public Transaction.TransactionType getType() {
    return type;
  }

  /**
   * Verifica se a transação é uma compra
   *
   * @return true se for compra
   */
  public boolean isBuyTransaction() {
    return type == Transaction.TransactionType.BUY;
  }

  /**
   * Verifica se a transação é uma venda
   *
   * @return true se for venda
   */
  public boolean isSellTransaction() {
    return type == Transaction.TransactionType.SELL;
  }
}
