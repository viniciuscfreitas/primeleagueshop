package com.primeleague.shop.models;

import java.sql.Timestamp;

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

  private String playerName;
  private ShopItem item;
  private int quantity;
  private double price;
  private TransactionType type;
  private Timestamp timestamp;
  private boolean success;

  /**
   * Cria uma nova transação
   *
   * @param playerName Nome do jogador
   * @param item       Item da transação
   * @param quantity   Quantidade
   * @param price      Preço
   * @param type       Tipo de transação (compra/venda)
   * @param timestamp  Timestamp da transação
   */
  public Transaction(String playerName, ShopItem item, int quantity, double price, TransactionType type, Timestamp timestamp) {
    this.playerName = playerName;
    this.item = item;
    this.quantity = quantity;
    this.price = price;
    this.type = type;
    this.timestamp = timestamp;
    this.success = false;
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

  public double getPrice() {
    return price;
  }

  public TransactionType getType() {
    return type;
  }

  public Timestamp getTimestamp() {
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

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    return String.format("[%s] %s %s %dx %s por $%.2f",
        timestamp,
        playerName,
        (type == TransactionType.BUY ? "comprou" : "vendeu"),
        quantity,
        item.getName(),
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
}

