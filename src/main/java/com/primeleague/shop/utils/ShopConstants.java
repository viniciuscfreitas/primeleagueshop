package com.primeleague.shop.utils;

/**
 * Constantes utilizadas no plugin
 */
public class ShopConstants {

    // Comandos
    public static final String CMD_BUY = "comprar";
    public static final String CMD_SELL = "vender";
    public static final String CMD_CATEGORIES = "categorias";
    public static final String CMD_HELP = "ajuda";
    public static final String CMD_ADMIN = "admin";
    public static final String CMD_RELOAD = "recarregar";
    public static final String CMD_LIST = "lista";
    public static final String CMD_SELLALL = "vender-tudo";
    public static final String CMD_HISTORY = "historico";
    public static final String CMD_TOP = "top";
    public static final String CMD_PREVIEW = "visualizar";
    public static final String CMD_FAVORITES = "favoritos";

    // Permissões
    public static final String PERM_BUY = "primeleagueshop.buy";
    public static final String PERM_SELL = "primeleagueshop.sell";
    public static final String PERM_ADMIN = "primeleagueshop.admin";
    public static final String PERM_USE = "primeleague.shop.use";
    public static final String PERM_BYPASS_CONFIRM = "primeleague.shop.bypass.confirm";

    // GUI - Layout Principal
    public static final int GUI_ROWS = 6;
    public static final int GUI_SLOTS = GUI_ROWS * 9;
    public static final int SEARCH_SLOT = 4;
    public static final int BALANCE_SLOT = 49;
    public static final int CART_SLOT = 46;
    public static final int HISTORY_SLOT = 47;
    public static final int FAVORITES_SLOT = 48;
    public static final int BACK_SLOT = 45;
    public static final int PREV_PAGE_SLOT = 48;
    public static final int NEXT_PAGE_SLOT = 50;

    // GUI - Layout de Categoria
    public static final int CATEGORY_ROWS = 6;
    public static final int CATEGORY_SLOTS = CATEGORY_ROWS * 9;
    public static final int FIRST_ITEM_SLOT = 0;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int CATEGORY_BACK_SLOT = 45;
    public static final int CATEGORY_PREV_SLOT = 48;
    public static final int CATEGORY_NEXT_SLOT = 50;
    public static final int PREVIEW_SLOT = 49;

    // GUI - Layout de Confirmação
    public static final int CONFIRM_ROWS = 5;
    public static final int CONFIRM_SLOTS = CONFIRM_ROWS * 9;
    public static final int CONFIRM_BUTTON_SLOT = 11;
    public static final int CANCEL_BUTTON_SLOT = 15;
    public static final int INFO_SLOT = 13;
    public static final int DECREASE_SLOT = 29;
    public static final int QUANTITY_SLOT = 31;
    public static final int INCREASE_SLOT = 33;

    // Materiais e Data Values
    public static final String MATERIAL_BACK = "ARROW";
    public static final String MATERIAL_NEXT = "PAPER";
    public static final String MATERIAL_PREV = "PAPER";
    public static final String MATERIAL_CONFIRM = "WOOL";
    public static final String MATERIAL_CANCEL = "WOOL";
    public static final String MATERIAL_INCREASE = "EMERALD";
    public static final String MATERIAL_DECREASE = "REDSTONE";
    public static final String MATERIAL_INFO = "SIGN";
    public static final String MATERIAL_QUANTITY = "INK_SACK";
    public static final String MATERIAL_FILLER = "STAINED_GLASS_PANE";
    public static final String MATERIAL_BACK_BUTTON = "ARROW";
    public static final String MATERIAL_PREVIOUS_PAGE = "PAPER";
    public static final String MATERIAL_NEXT_PAGE = "PAPER";

    public static final byte DATA_CONFIRM = 5;  // Verde
    public static final byte DATA_CANCEL = 14;  // Vermelho
    public static final byte DATA_FILL = 15;    // Preto
    public static final byte DATA_INCREASE = 10; // Verde
    public static final byte DATA_DECREASE = 1;  // Vermelho

    // Configurações
    public static final String CONFIG_CURRENCY_SYMBOL = "moeda.simbolo";
    public static final String CONFIG_MESSAGES_PREFIX = "mensagens.";

    // Mensagens
    public static final String MSG_PREFIX = "&8[&aLoja&8] ";
    public static final String MSG_RELOADED = "&aPlugin recarregado com sucesso!";
    public static final String MSG_NO_PERM = "&cVocê não tem permissão para isso.";
    public static final String MSG_INVALID_AMOUNT = "&cQuantidade inválida! Use um número entre 1 e {max}.";
    public static final String MSG_NOT_ENOUGH_MONEY = "&cVocê não tem dinheiro suficiente! Necessário: {price}";
    public static final String MSG_NOT_ENOUGH_ITEMS = "&cVocê não tem itens suficientes para vender!";
    public static final String MSG_INVENTORY_FULL = "&cSeu inventário está cheio!";
    public static final String MSG_PURCHASE_SUCCESS = "&aCompra realizada com sucesso!";
    public static final String MSG_SALE_SUCCESS = "&aVenda realizada com sucesso!";

    // Logs
    public static final String LOG_TRANSACTION = "[Transação] %s";
    public static final String LOG_DATABASE_ERROR = "Erro no banco de dados: %s";
    public static final String LOG_CONFIG_ERROR = "[Loja] Erro de configuração: %s";
    public static final String LOG_PLUGIN_DISABLED = "Plugin PrimeLeagueShop desativado com sucesso!";
    public static final String LOG_CLICK = "Menu principal: Clique no slot %d, categoria=%s";
    public static final String LOG_OPEN = "Menu principal: Abrindo para %s com %d categorias";
    public static final String LOG_CLICK_ERROR = "Erro ao processar clique na GUI da categoria: %s";
    public static final String LOG_OPENING_CATEGORY = "Abrindo categoria %s para jogador %s na página %d";
    public static final String LOG_FEEDBACK_ERROR = "Erro ao processar feedback: %s";
    public static final String LOG_DATABASE_CONNECT = "Conectando ao banco de dados...";
    public static final String LOG_DATABASE_RECONNECTED = "Reconectado ao banco de dados com sucesso";
    public static final String LOG_DATABASE_RECONNECT = "Tentando reconectar ao banco de dados...";

    // Sistema Econômico
    public static final double DEFAULT_BUY_PRICE = 100.0;
    public static final double DEFAULT_SELL_PRICE = 50.0;
    public static final int DEFAULT_MAX_BUY_QUANTITY = 64;
    public static final int DEFAULT_MAX_SELL_QUANTITY = 64;
    public static final boolean DEFAULT_USE_VAULT = true;

    private ShopConstants() {
        // Construtor privado para evitar instanciação
    }
}
