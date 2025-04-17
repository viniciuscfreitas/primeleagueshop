# PrimeLeagueShop

Sistema de loja oficial do servidor Prime League para Spigot 1.5.2.

## 📋 Características

- Loja com interface gráfica organizada por categorias
- Sistema de compra e venda de itens
- Economia integrada com Vault
- Confirmação para transações de alto valor
- Sistema de permissões por categoria e item
- Paginação para navegação fácil
- Configuração flexível via arquivos YAML
- Registro de transações
- Cache em memória para melhor performance
- Compatível com Spigot 1.5.2 e Java 8

## 🔧 Instalação

1. Baixe o arquivo JAR da última release
2. Coloque o arquivo JAR na pasta `plugins` do seu servidor
3. Reinicie o servidor ou use `/reload`
4. Edite os arquivos de configuração em `plugins/PrimeLeagueShop/`

## ⚙️ Configuração

O plugin cria automaticamente os seguintes arquivos de configuração:

- `config.yml` - Configurações gerais do plugin
- `shop.yml` - Categorias e itens da loja
- `messages.yml` - Mensagens e textos da interface

### Adicionando Categorias e Itens

Edite o arquivo `shop.yml` para adicionar novas categorias e itens:

```yaml
categories:
  - name: "Minérios"
    icon: "DIAMOND_ORE"
    slot: 10
    permission: "primeleague.shop.ores"
    items:
      - name: "Diamante"
        material: "DIAMOND"
        data: 0
        buy_price: 100
        sell_price: 50
        permission: ""
        lore:
          - "&7Um diamante brilhante"
          - "&aPreço de compra: &f{buy_price}{currency}"
          - "&cPreço de venda: &f{sell_price}{currency}"
```

## 🔍 Comandos

- `/shop` - Abre a loja principal
- `/shop buy <item> <qtd>` - Compra uma quantidade específica de um item
- `/shop sell <item> <qtd>` - Vende uma quantidade específica de um item
- `/shop reload` - Recarrega as configurações do plugin
- `/shop admin` - Exibe informações administrativas

## 🔒 Permissões

- `primeleague.shop.use` - Permissão para usar a loja
- `primeleague.shop.buy` - Permissão para comprar itens
- `primeleague.shop.sell` - Permissão para vender itens
- `primeleague.shop.admin` - Permissão para comandos administrativos
- `primeleague.shop.bypass.confirm` - Ignora a confirmação para transações de alto valor
- `primeleague.shop.<categoria>` - Permissão para acessar uma categoria específica

## 🔄 Dependências

- Vault - Para integração com economia
- Um plugin de economia como iConomy

## 🏗️ Compilando o Código Fonte

```bash
git clone https://github.com/primeleague/shop.git
cd shop
mvn clean package
```

O plugin compilado estará disponível em `target/shop-1.0.0.jar`

## 📜 Licença

Este projeto é distribuído sob a licença MIT. Veja o arquivo LICENSE para mais detalhes.

## 📞 Suporte

Para suporte, reporte bugs ou sugestões, abra uma issue no GitHub ou contate os administradores do servidor Prime League.
