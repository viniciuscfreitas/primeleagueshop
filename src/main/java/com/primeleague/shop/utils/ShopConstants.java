package com.primeleague.shop.utils;

/**
 * Constantes usadas no plugin
 */
public class ShopConstants {

  // Permissões
  public static final String PERM_USE = "primeleague.shop.use";
  public static final String PERM_BUY = "primeleagueshop.buy";
  public static final String PERM_SELL = "primeleagueshop.sell";
  public static final String PERM_ADMIN = "primeleagueshop.admin";
  public static final String PERM_BYPASS_CONFIRM = "primeleague.shop.bypass.confirm";

  // Comandos
  public static final String CMD_SHOP = "shop";
  public static final String CMD_BUY = "buy";
  public static final String CMD_SELL = "sell";
  public static final String CMD_RELOAD = "reload";
  public static final String CMD_ADMIN = "admin";
  public static final String CMD_HELP = "help";

  // GUI
  public static final int ROWS = 6;
  public static final int SLOTS = ROWS * 9;

  public static final int BACK_SLOT = 45;
  public static final int PREV_PAGE_SLOT = 48;
  public static final int NEXT_PAGE_SLOT = 50;

  // Botões de confirmação
  public static final int CONFIRM_BUTTON_SLOT = 11;
  public static final int CANCEL_BUTTON_SLOT = 15;
  public static final int INFO_SLOT = 13;

  // Slots de quantidade
  public static final int DECREASE_SLOT = 29;
  public static final int QUANTITY_SLOT = 31;
  public static final int INCREASE_SLOT = 33;

  // Sistema de economia
  public static final double DEFAULT_BUY_PRICE = 100.0;
  public static final double DEFAULT_SELL_PRICE = 50.0;
  public static final int DEFAULT_MAX_BUY_QUANTITY = 64;
  public static final int DEFAULT_MAX_SELL_QUANTITY = 64;

  // Configurações
  public static final String DEFAULT_CURRENCY_SYMBOL = "$";
  public static final boolean DEFAULT_USE_VAULT = true;

  // Materiais comuns
  public static final String MATERIAL_BACK_BUTTON = "ARROW";
  public static final String MATERIAL_NEXT_PAGE = "PAPER";
  public static final String MATERIAL_PREVIOUS_PAGE = "PAPER";
  public static final String MATERIAL_CONFIRM = "WOOL";
  public static final String MATERIAL_CANCEL = "WOOL";
  public static final String MATERIAL_INCREASE = "EMERALD";
  public static final String MATERIAL_DECREASE = "REDSTONE";
  public static final String MATERIAL_INFO = "SIGN";

  // Data values
  public static final byte DATA_CONFIRM = 5; // Verde
  public static final byte DATA_CANCEL = 14; // Vermelho
  public static final byte DATA_FILL = 15; // Preto

  // Mensagens
  public static final String MSG_PREFIX = "&8[&aShop&8] ";
  public static final String MSG_RELOADED = "&aPlugin recarregado com sucesso!";
  public static final String MSG_NO_PERM = "&cVocê não tem permissão para isso.";

  // Mensagens de Log
  public static final String LOG_TRANSACTION = "[Transação] %s";
  public static final String LOG_DATABASE_ERROR = "Erro no banco de dados: %s";
  public static final String LOG_DATABASE_CONNECT = "Conectado ao banco de dados MySQL";
  public static final String LOG_DATABASE_RECONNECT = "Tentativa %d de reconexão falhou";
  public static final String LOG_DATABASE_RECONNECTED = "Reconectado ao banco de dados";
  public static final String LOG_FEEDBACK_ERROR = "Erro ao executar feedback: %s";
  public static final String LOG_CONFIG_ERROR = "Erro ao carregar configurações: %s";

  private ShopConstants() {
    // Classe de constantes, não deve ser instanciada
  }
}
