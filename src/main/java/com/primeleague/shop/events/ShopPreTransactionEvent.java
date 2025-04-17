package com.primeleague.shop.events;

import com.primeleague.shop.models.ShopItem;
import com.primeleague.shop.models.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado antes de uma transação na loja ser concluída
 * Pode ser cancelado para impedir a transação
 */
public class ShopPreTransactionEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();

  private final Player player;
  private final ShopItem item;
  private int quantity;
  private double totalPrice;
  private final Transaction.TransactionType type;
  private boolean cancelled;
  private String cancelReason;

  /**
   * Cria um novo evento de pré-transação
   *
   * @param player     Jogador
   * @param item       Item da transação
   * @param quantity   Quantidade
   * @param totalPrice Preço total
   * @param type       Tipo de transação
   */
  public ShopPreTransactionEvent(Player player, ShopItem item, int quantity, double totalPrice,
      Transaction.TransactionType type) {
    this.player = player;
    this.item = item;
    this.quantity = quantity;
    this.totalPrice = totalPrice;
    this.type = type;
    this.cancelled = false;
    this.cancelReason = "";
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  /**
   * Define o motivo do cancelamento
   *
   * @param reason Motivo
   */
  public void setCancelReason(String reason) {
    this.cancelReason = reason;
    if (reason != null && !reason.isEmpty()) {
      setCancelled(true);
    }
  }

  /**
   * Obtém o motivo do cancelamento
   *
   * @return Motivo ou string vazia
   */
  public String getCancelReason() {
    return cancelReason;
  }

  /**
   * Obtém o jogador que está realizando a transação
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
   * Define a quantidade da transação
   *
   * @param quantity Nova quantidade
   */
  public void setQuantity(int quantity) {
    if (quantity <= 0)
      throw new IllegalArgumentException("Quantidade deve ser positiva");
    this.quantity = quantity;
    recalculatePrice();
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
   * Define o preço total da transação
   *
   * @param totalPrice Novo preço total
   */
  public void setTotalPrice(double totalPrice) {
    if (totalPrice < 0)
      throw new IllegalArgumentException("Preço deve ser não-negativo");
    this.totalPrice = totalPrice;
  }

  /**
   * Recalcula o preço baseado na quantidade
   */
  private void recalculatePrice() {
    if (type == Transaction.TransactionType.BUY) {
      this.totalPrice = item.getBuyPrice() * quantity;
    } else {
      this.totalPrice = item.getSellPrice() * quantity;
    }
  }

  /**
   * Obtém o tipo da transação
   *
   * @return Tipo de transação
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
