package com.primeleague.shop.models;

/**
 * Representa uma transação na loja
 */
public class Transaction {

  /**
   * Tipo de transação
   */
  public enum TransactionType {
    BUY, SELL
  }

  private final String playerName;
  private final ShopItem item;
  private final int quantity;
  private final double unitPrice;
  private final TransactionType type;
  private boolean successful;
  private final long timestamp;

  /**
   * Cria uma nova transação
   *
   * @param playerName Nome do jogador
   * @param item       Item da transação
   * @param quantity   Quantidade
   * @param unitPrice  Preço por unidade
   * @param type       Tipo de transação (compra/venda)
   */
  public Transaction(String playerName, ShopItem item, int quantity, double unitPrice, TransactionType type) {
    this.playerName = playerName;
    this.item = item;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.type = type;
    this.successful = false;
    this.timestamp = System.currentTimeMillis();
  }

  // Getters

  public String getPlayerName() {
    return playerName;
  }

  public ShopItem getItem() {
    return item;
  }

  public int getQuantity() {
    return quantity;
  }

  public double getUnitPrice() {
    return unitPrice;
  }

  public double getTotalPrice() {
    return unitPrice * quantity;
  }

  public TransactionType getType() {
    return type;
  }

  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Marca a transação como bem-sucedida
   */
  public void markSuccessful() {
    this.successful = true;
  }

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Retorna se esta é uma transação de compra
   *
   * @return true se for compra, false se for venda
   */
  public boolean isBuyTransaction() {
    return type == TransactionType.BUY;
  }

  /**
   * Retorna se esta é uma transação de venda
   *
   * @return true se for venda, false se for compra
   */
  public boolean isSellTransaction() {
    return type == TransactionType.SELL;
  }

  @Override
  public String toString() {
    return String.format("[%s] %s %s %dx %s a %.2f cada (Total: %.2f)",
        timestamp, playerName, (type == TransactionType.BUY ? "comprou" : "vendeu"),
        quantity, item.getName(), unitPrice, getTotalPrice());
  }
}
