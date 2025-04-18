package com.primeleague.shop.utils;

/**
 * Constantes utilizadas no plugin
 */
public class ShopConstants {

    // Comandos
    public static final String CMD_BUY = "buy";
    public static final String CMD_SELL = "sell";
    public static final String CMD_CATEGORIES = "categories";
    public static final String CMD_HELP = "help";
    public static final String CMD_ADMIN = "admin";
    public static final String CMD_RELOAD = "reload";

    // Permissões
    public static final String PERM_BUY = "primeleagueshop.buy";
    public static final String PERM_SELL = "primeleagueshop.sell";
    public static final String PERM_ADMIN = "primeleagueshop.admin";
    public static final String PERM_USE = "primeleague.shop.use";
    public static final String PERM_BYPASS_CONFIRM = "primeleague.shop.bypass.confirm";

    // GUI
    public static final int ROWS = 4;
    public static final int SLOTS = ROWS * 9;
    public static final int BACK_SLOT = 27;
    public static final int PREV_PAGE_SLOT = 30;
    public static final int NEXT_PAGE_SLOT = 32;
    public static final int CONFIRM_BUTTON_SLOT = 11;
    public static final int CANCEL_BUTTON_SLOT = 15;
    public static final int INFO_SLOT = 13;
    public static final int DECREASE_SLOT = 20;
    public static final int QUANTITY_SLOT = 22;
    public static final int INCREASE_SLOT = 24;

    // Materiais e seus dados
    public static final String MATERIAL_BACK_BUTTON = "ARROW";
    public static final String MATERIAL_NEXT_PAGE = "PAPER";
    public static final String MATERIAL_PREVIOUS_PAGE = "PAPER";
    public static final String MATERIAL_CONFIRM = "WOOL";
    public static final String MATERIAL_CANCEL = "WOOL";
    public static final String MATERIAL_INCREASE = "EMERALD";
    public static final String MATERIAL_DECREASE = "REDSTONE";
    public static final String MATERIAL_INFO = "SIGN";
    public static final String MATERIAL_QUANTITY = "INK_SACK";
    public static final String MATERIAL_FILLER = "STAINED_GLASS_PANE";

    // Data values
    public static final short DATA_CONFIRM = 5; // Verde
    public static final short DATA_CANCEL = 14; // Vermelho
    public static final short DATA_FILL = 15; // Preto
    public static final short DATA_INCREASE = 10; // Verde
    public static final short DATA_DECREASE = 1; // Vermelho

    // Configurações
    public static final String CONFIG_CURRENCY_SYMBOL = "currency.symbol";
    public static final String CONFIG_MESSAGES_PREFIX = "messages.";

    // Mensagens padrão
    public static final String DEFAULT_CURRENCY_SYMBOL = "$";
    public static final String DEFAULT_MESSAGE_PREFIX = "&8[&6Shop&8] &7";
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
    public static final String LOG_CONFIG_ERROR = "[Shop] Erro de configuração: %s";

    // Sistema de economia
    public static final double DEFAULT_BUY_PRICE = 100.0;
    public static final double DEFAULT_SELL_PRICE = 50.0;
    public static final int DEFAULT_MAX_BUY_QUANTITY = 64;
    public static final int DEFAULT_MAX_SELL_QUANTITY = 64;
    public static final boolean DEFAULT_USE_VAULT = true;

    private ShopConstants() {
        // Construtor privado para evitar instanciação
    }
}
