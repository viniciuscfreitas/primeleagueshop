#!/bin/bash

# Script de deploy automático para o PrimeLeagueShop
# AVISO: Este script contém senha em texto puro, o que não é recomendado para uso em produção
# Para maior segurança, configure autenticação por chave SSH

# Configurações
SERVER_IP="181.215.45.238"
SERVER_USER="root"
SERVER_PASS="rianzada"
PLUGIN_DIR="/home/minecraft/server/plugins"
PLUGIN_NAME="shop-1.0.0.jar"

# Cores para saída
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Iniciando deploy do plugin PrimeLeagueShop...${NC}"

# Compilar o projeto
echo -e "${GREEN}Compilando projeto com Maven...${NC}"
mvn clean package

# Verificar se a compilação foi bem sucedida
if [ $? -ne 0 ]; then
    echo -e "${RED}Erro na compilação do projeto! Abortando deploy.${NC}"
    exit 1
fi

# Path do arquivo JAR gerado
JAR_PATH="target/$PLUGIN_NAME"

# Verificar se o arquivo JAR existe
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}Arquivo JAR não encontrado em $JAR_PATH! Verifique o nome do arquivo.${NC}"
    exit 1
fi

echo -e "${GREEN}Arquivo JAR compilado com sucesso: $JAR_PATH${NC}"

# Instalar sshpass se não estiver disponível (necessário para uso com senha)
if ! command -v sshpass &> /dev/null; then
    echo -e "${GREEN}Instalando sshpass...${NC}"
    brew install sshpass || { echo -e "${RED}Falha ao instalar sshpass. Instale manualmente.${NC}"; exit 1; }
fi

# Upload do arquivo para o servidor
echo -e "${GREEN}Enviando plugin para o servidor...${NC}"
sshpass -p "$SERVER_PASS" scp "$JAR_PATH" "$SERVER_USER@$SERVER_IP:$PLUGIN_DIR/"

# Verificar se o upload foi bem sucedido
if [ $? -ne 0 ]; then
    echo -e "${RED}Erro ao enviar arquivo para o servidor! Verifique as credenciais e conexão.${NC}"
    exit 1
fi

# Criar pasta de configurações e enviar arquivos YAML
echo -e "${GREEN}Enviando arquivos de configuração...${NC}"
CONFIG_DIR="$PLUGIN_DIR/PrimeLeagueShop"
sshpass -p "$SERVER_PASS" ssh "$SERVER_USER@$SERVER_IP" "mkdir -p $CONFIG_DIR"
sshpass -p "$SERVER_PASS" scp "src/main/resources/config.yml" "$SERVER_USER@$SERVER_IP:$CONFIG_DIR/config.yml"
sshpass -p "$SERVER_PASS" scp "src/main/resources/shop.yml" "$SERVER_USER@$SERVER_IP:$CONFIG_DIR/shop.yml"
sshpass -p "$SERVER_PASS" scp "src/main/resources/messages.yml" "$SERVER_USER@$SERVER_IP:$CONFIG_DIR/messages.yml"

# Reiniciar o servidor Minecraft (opcional, descomente se necessário)
echo -e "${GREEN}Reiniciando o servidor Minecraft...${NC}"
sshpass -p "$SERVER_PASS" ssh "$SERVER_USER@$SERVER_IP" "cd /home/minecraft/server && ./restart.sh"

echo -e "${GREEN}Deploy concluído com sucesso!${NC}"
