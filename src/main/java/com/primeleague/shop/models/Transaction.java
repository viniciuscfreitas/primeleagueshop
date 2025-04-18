package com.primeleague.shop.models;

import java.util.UUID;

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

  private final UUID id;
  private final String playerName;
  private final String itemName;
  private final int quantity;
  private final double price;
  private final boolean isBuy;
  private final long timestamp;
  private boolean success;

  /**
   * Cria uma nova transação
   *
   * @param playerName Nome do jogador
   * @param itemName   Nome do item
   * @param quantity   Quantidade
   * @param price      Preço
   * @param isBuy      true se for compra, false se for venda
   */
  public Transaction(String playerName, String itemName, int quantity, double price, boolean isBuy) {
    this.id = UUID.randomUUID();
    this.playerName = playerName;
    this.itemName = itemName;
    this.quantity = quantity;
    this.price = price;
    this.isBuy = isBuy;
    this.timestamp = System.currentTimeMillis();
    this.success = false;
  }

  /**
   * Cria uma nova transação com timestamp específico
   */
  public Transaction(String playerName, String itemName, int quantity, double price, long timestamp, boolean isBuy) {
    this.id = UUID.randomUUID();
    this.playerName = playerName;
    this.itemName = itemName;
    this.quantity = quantity;
    this.price = price;
    this.isBuy = isBuy;
    this.timestamp = timestamp;
    this.success = false;
  }

  /**
   * Cria uma nova transação com item e tipo
   */
  public Transaction(String playerName, ShopItem item, int quantity, double price, TransactionType type, java.sql.Timestamp timestamp) {
    this.id = UUID.randomUUID();
    this.playerName = playerName;
    this.itemName = item.getName();
    this.quantity = quantity;
    this.price = price;
    this.isBuy = type == TransactionType.BUY;
    this.timestamp = timestamp.getTime();
    this.success = false;
  }

  // Getters

  public UUID getId() {
    return id;
  }

  public String getPlayerName() {
    return playerName;
  }

  public String getItemName() {
    return itemName;
  }

  public int getQuantity() {
    return quantity;
  }

  public double getPrice() {
    return price;
  }

  public boolean isBuy() {
    return isBuy;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public boolean isSuccess() {
    return success;
  }

  /**
   * Marca a transação como bem-sucedida
   */
  public void markSuccessful() {
    this.success = true;
  }

  /**
   * Retorna se esta é uma transação de compra
   *
   * @return true se for compra, false se for venda
   */
  public boolean isBuyTransaction() {
    return isBuy;
  }

  /**
   * Retorna se esta é uma transação de venda
   *
   * @return true se for venda, false se for compra
   */
  public boolean isSellTransaction() {
    return !isBuy;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    return String.format("[%s] %s %s %dx %s por $%.2f",
        timestamp,
        playerName,
        (isBuy ? "comprou" : "vendeu"),
        quantity,
        itemName,
        getTotalPrice());
  }

  public double getTotalPrice() {
    return price * quantity;
  }

  /**
   * Retorna o preço unitário da transação
   * @return preço por unidade
   */
  public double getUnitPrice() {
    return price;
  }

  /**
   * Verifica se a transação foi bem-sucedida
   * @return true se a transação foi concluída com sucesso
   */
  public boolean isSuccessful() {
    return success;
  }

  /**
   * Retorna o tipo da transação
   * @return TransactionType.BUY se for compra, TransactionType.SELL se for venda
   */
  public TransactionType getType() {
    return isBuy ? TransactionType.BUY : TransactionType.SELL;
  }

  /**
   * Retorna o nome do item da transação
   * @return nome do item
   */
  public String getItem() {
    return itemName;
  }
}

