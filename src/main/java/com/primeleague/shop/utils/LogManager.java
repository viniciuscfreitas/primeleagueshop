package com.primeleague.shop.utils;

import com.primeleague.shop.PrimeLeagueShopPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogManager {
  private final PrimeLeagueShopPlugin plugin;
  private final File logFolder;
  private final SimpleDateFormat dateFormat;
  private final Queue<LogEntry> logQueue;
  private FileWriter currentWriter;
  private String currentDate;
  private boolean asyncLogging;

  private static class LogEntry {
    private final String message;
    private final LogType type;
    private final long timestamp;

    public LogEntry(String message, LogType type) {
      this.message = message;
      this.type = type;
      this.timestamp = System.currentTimeMillis();
    }
  }

  public enum LogType {
    TRANSACTION("transactions"),
    PRICE("prices"),
    ERROR("errors"),
    SECURITY("security"),
    PERFORMANCE("performance");

    private final String fileName;

    LogType(String fileName) {
      this.fileName = fileName;
    }

    public String getFileName() {
      return fileName;
    }
  }

  public LogManager(PrimeLeagueShopPlugin plugin) {
    this.plugin = plugin;
    this.logFolder = new File(plugin.getDataFolder(), "logs");
    this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    this.logQueue = new ConcurrentLinkedQueue<>();
    this.asyncLogging = true;

    // Cria pasta de logs
    if (!logFolder.exists()) {
      logFolder.mkdirs();
    }

    // Inicia processamento assíncrono de logs
    startAsyncLogging();
  }

  private void startAsyncLogging() {
    plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
      @Override
      public void run() {
        processLogQueue();
      }
    }, 100L, 100L); // A cada 5 segundos
  }

  private void processLogQueue() {
    if (!asyncLogging || logQueue.isEmpty()) {
      return;
    }

    try {
      LogEntry entry;
      while ((entry = logQueue.poll()) != null) {
        writeLog(entry);
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Erro ao processar fila de logs", e);
    }
  }

  private void writeLog(LogEntry entry) {
    String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(entry.timestamp));
    File logFile = new File(logFolder, date + "_" + entry.type.getFileName() + ".log");

    try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
      writer.println(String.format("[%s] %s",
        dateFormat.format(new Date(entry.timestamp)),
        entry.message
      ));
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Erro ao escrever log", e);
    }
  }

  public void log(String message, LogType type) {
    if (asyncLogging) {
      logQueue.offer(new LogEntry(message, type));
    } else {
      writeLog(new LogEntry(message, type));
    }
  }

  public void logTransaction(String player, String action, String item, int quantity, double price) {
    log(String.format("%s %s %dx %s por %.2f",
      player, action, quantity, item, price
    ), LogType.TRANSACTION);
  }

  public void logPrice(String item, double oldPrice, double newPrice, int demand) {
    log(String.format("Item: %s, Preço: %.2f -> %.2f, Demanda: %d",
      item, oldPrice, newPrice, demand
    ), LogType.PRICE);
  }

  public void logError(String message, Throwable error) {
    StringBuilder sb = new StringBuilder(message);
    if (error != null) {
      sb.append("\n").append(error.getMessage());
      for (StackTraceElement element : error.getStackTrace()) {
        sb.append("\n  at ").append(element.toString());
      }
    }
    log(sb.toString(), LogType.ERROR);
  }

  public void logSecurity(String player, String action, String details) {
    log(String.format("%s realizou %s - %s",
      player, action, details
    ), LogType.SECURITY);
  }

  public void logPerformance(String operation, long duration) {
    log(String.format("Operação: %s, Duração: %dms",
      operation, duration
    ), LogType.PERFORMANCE);
  }

  public void shutdown() {
    // Processa logs pendentes
    asyncLogging = false;
    processLogQueue();
  }
}
