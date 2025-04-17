package com.primeleague.shop.events;

import com.primeleague.shop.models.ShopCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado quando um jogador abre uma categoria da loja
 * Pode ser cancelado para impedir a abertura
 */
public class CategoryOpenEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();

  private final Player player;
  private final ShopCategory category;
  private boolean cancelled;
  private String cancelReason;

  /**
   * Cria um novo evento de abertura de categoria
   *
   * @param player   Jogador que está abrindo a categoria
   * @param category Categoria sendo aberta
   */
  public CategoryOpenEvent(Player player, ShopCategory category) {
    this.player = player;
    this.category = category;
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
   * Obtém o jogador que está abrindo a categoria
   *
   * @return Jogador
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Obtém a categoria sendo aberta
   *
   * @return Categoria
   */
  public ShopCategory getCategory() {
    return category;
  }
}
