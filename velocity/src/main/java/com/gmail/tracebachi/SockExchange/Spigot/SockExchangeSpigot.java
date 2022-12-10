/*
 * SockExchange - Server and Client for BungeeCord and Spigot communication
 * Copyright (C) 2017 tracebachi@gmail.com (GeeItsZee)
 *
 * spigotPlugin program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * spigotPlugin program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with spigotPlugin program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.SockExchange.Spigot;

import com.gmail.tracebachi.SockExchange.ExpirableConsumer;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessageNotifier;
import com.gmail.tracebachi.SockExchange.Messages.ResponseMessage;
import com.gmail.tracebachi.SockExchange.Messages.ResponseStatus;
import com.gmail.tracebachi.SockExchange.Netty.SockExchangeClient;
import com.gmail.tracebachi.SockExchange.Netty.SpigotToVelocityConnection;
import com.gmail.tracebachi.SockExchange.Scheduler.AwaitableExecutor;
import com.gmail.tracebachi.SockExchange.Scheduler.ScheduledExecutorServiceWrapper;
import com.gmail.tracebachi.SockExchange.SpigotServerInfo;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.JulBasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.LongIdCounterMap;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lv.exosmium.exoplaytime.spigot.SpigotPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class SockExchangeSpigot implements SpigotTieIn
{
  private final SockExchangeConfiguration configuration = new SockExchangeConfiguration();

  private ScheduledThreadPoolExecutor threadPoolExecutor;
  private AwaitableExecutor awaitableExecutor;
  private BasicLogger basicLogger;
  private ReceivedMessageNotifier messageNotifier;
  private LongIdCounterMap<ExpirableConsumer<ResponseMessage>> responseConsumerMap;
  private SpigotToVelocityConnection connection;
  private SockExchangeClient sockExchangeClient;
  private ScheduledFuture<?> consumerTimeoutCleanupFuture;

  private PlayerUpdateChannelListener playerUpdateChannelListener;
  private KeepAliveChannelListener keepAliveChannelListener;
  private MoveOtherToCommand moveOtherToCommand;
  private MoveToCommand moveToCommand;
  private RunCmdCommand runCmdCommand;
  private ChatMessageChannelListener chatMessageChannelListener;
  private RunCmdChannelListener runCmdChannelListener;
  private SpigotKeepAliveSender spigotKeepAliveSender;

  private YamlFile yamlConfig;
  private String yamlName = "client-socket-config.yml";
  private SpigotPlugin spigotPlugin;

  public SockExchangeSpigot(SpigotPlugin spigotPlugin) {
    this.spigotPlugin = spigotPlugin;
  }


  public void loadSocket() {
    spigotPlugin.saveDefaultConfig();
  }

  private void setupConfig() {
    try {
      this.yamlConfig = new YamlFile(spigotPlugin.getDataFolder() + File.separator + yamlName);
      if (!yamlConfig.exists()) {
        yamlConfig.createNewFile();
        writeDefaultConfig();
      }
      yamlConfig.load(spigotPlugin.getDataFolder() + File.separator + yamlName);
    } catch (IOException io) {
      io.printStackTrace();
    }
  }

  private void writeDefaultConfig() throws IOException {
    InputStream stream = this.getClass().getResourceAsStream("/" + yamlName);
    BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
    OutputStream outputStream = new FileOutputStream(yamlConfig.getConfigurationFile());
    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

    bufferedOutputStream.write(bufferedInputStream.readAllBytes());
    bufferedOutputStream.close();
    outputStream.close();
    bufferedInputStream.close();
  }
  
  public void initSocket() {
    setupConfig();
    spigotPlugin.reloadConfig();
    configuration.read(yamlConfig);

    boolean debugMode = configuration.inDebugMode();
    String hostName = configuration.getHostName();
    int port = configuration.getPort();
    String serverName = configuration.getServerName();
    String registrationPassword = configuration.getRegistrationPassword();
    MessageFormatMap messageFormatMap = configuration.getMessageFormatMap();

    // Create the logger based on Java.Util.Logging
    basicLogger = new JulBasicLogger(spigotPlugin.getLogger(), debugMode);

    // Create the shared thread pool executor
    buildThreadPoolExecutor();
    ScheduledExecutorServiceWrapper wrappedThreadPool =
      new ScheduledExecutorServiceWrapper(threadPoolExecutor);

    // Create the AwaitableExecutor
    awaitableExecutor = new AwaitableExecutor(wrappedThreadPool);

    // Create the message notifier which will run consumers on SockExchange messages
    messageNotifier = new ReceivedMessageNotifier(awaitableExecutor);

    // Create the map that manages consumers for responses to sent message
    responseConsumerMap = new LongIdCounterMap<>();

    // Schedule a task to clean up the responseConsumerMap (handling timeouts)
    consumerTimeoutCleanupFuture = threadPoolExecutor.scheduleWithFixedDelay(
      this::checkForConsumerTimeouts, 5, 5, TimeUnit.SECONDS);

    // Create the Spigot-to-Bungee connection
    connection = new SpigotToVelocityConnection(
      serverName, registrationPassword, awaitableExecutor, messageNotifier, responseConsumerMap,
      basicLogger);

    // Create the API
    SockExchangeApi api = new SockExchangeApi(
      this, threadPoolExecutor, messageNotifier, connection);
    SockExchangeApi.setInstance(api);

    playerUpdateChannelListener = new PlayerUpdateChannelListener(api);
    playerUpdateChannelListener.register();

    keepAliveChannelListener = new KeepAliveChannelListener(api);
    keepAliveChannelListener.register();

    moveOtherToCommand = new MoveOtherToCommand(this, spigotPlugin, messageFormatMap, api);
    moveOtherToCommand.register();

    moveToCommand = new MoveToCommand(this, spigotPlugin, messageFormatMap, api);
    moveToCommand.register();

    runCmdChannelListener = new RunCmdChannelListener(this, spigotPlugin, basicLogger, api);
    runCmdChannelListener.register();

    runCmdCommand = new RunCmdCommand(spigotPlugin, runCmdChannelListener, messageFormatMap, api);
    runCmdCommand.register();

    chatMessageChannelListener = new ChatMessageChannelListener(this, spigotPlugin, api);
    chatMessageChannelListener.register();

    spigotKeepAliveSender = new SpigotKeepAliveSender(api, 2000);
    spigotKeepAliveSender.register();

    try
    {
      sockExchangeClient = new SockExchangeClient(hostName, port, connection);
      sockExchangeClient.start();
    }
    catch (Exception e)
    {
      spigotPlugin.getLogger().severe("============================================================");
      spigotPlugin.getLogger().severe("The SockExchange client could not be started. Refer to the stacktrace below.");
      spigotPlugin.getLogger().severe("Regardless, reconnects will be attempted.");
      e.printStackTrace();
      spigotPlugin.getLogger().severe("============================================================");
    }
  }

  public void disableSocket() {
    // Shut down the AwaitableExecutor first so tasks are not running
    // when shutting down everything else
    if (awaitableExecutor != null)
    {
      shutdownAwaitableExecutor();
      awaitableExecutor = null;
    }

    if (sockExchangeClient != null)
    {
      sockExchangeClient.shutdown();
      sockExchangeClient = null;
    }

    if (spigotKeepAliveSender != null)
    {
      spigotKeepAliveSender.unregister();
      spigotKeepAliveSender = null;
    }

    if (chatMessageChannelListener != null)
    {
      chatMessageChannelListener.unregister();
      chatMessageChannelListener = null;
    }

    if (runCmdCommand != null)
    {
      runCmdCommand.unregister();
      runCmdCommand = null;
    }

    if (runCmdChannelListener != null)
    {
      runCmdChannelListener.unregister();
      runCmdChannelListener = null;
    }

    if (moveToCommand != null)
    {
      moveToCommand.unregister();
      moveToCommand = null;
    }

    if (moveOtherToCommand != null)
    {
      moveOtherToCommand.unregister();
      moveOtherToCommand = null;
    }

    if (keepAliveChannelListener != null)
    {
      keepAliveChannelListener.unregister();
      keepAliveChannelListener = null;
    }

    if (playerUpdateChannelListener != null)
    {
      playerUpdateChannelListener.unregister();
      playerUpdateChannelListener = null;
    }

    SockExchangeApi.setInstance(null);

    connection = null;

    if (consumerTimeoutCleanupFuture != null)
    {
      consumerTimeoutCleanupFuture.cancel(false);
      consumerTimeoutCleanupFuture = null;
    }

    if (responseConsumerMap != null)
    {
      responseConsumerMap.clear();
      responseConsumerMap = null;
    }

    messageNotifier = null;
    basicLogger = null;

    if (threadPoolExecutor != null)
    {
      shutdownThreadPoolExecutor();
      threadPoolExecutor = null;
    }
  }

  @Override
  public SpigotServerInfo getServerInfo(String serverName)
  {
    return keepAliveChannelListener.getServerInfo(serverName);
  }

  @Override
  public Collection<SpigotServerInfo> getServerInfos()
  {
    return keepAliveChannelListener.getServerInfos();
  }

  @Override
  public Set<String> getOnlinePlayerNames()
  {
    return playerUpdateChannelListener.getOnlinePlayerNames();
  }

  @Override
  public void sendChatMessagesToConsole(List<String> messages)
  {
    spigotPlugin.getServer().getScheduler().runTask(spigotPlugin, () ->
    {
      CommandSender receiver = spigotPlugin.getServer().getConsoleSender();
      for (String message : messages)
      {
        receiver.sendMessage(message);
      }
    });
  }

  @Override
  public void isPlayerOnServer(String playerName, Consumer<Boolean> consumer)
  {
    spigotPlugin.getServer().getScheduler().runTask(spigotPlugin, () ->
    {
      Player player = spigotPlugin.getServer().getPlayerExact(playerName);
      consumer.accept(player != null);
    });
  }

  public void executeSync(Runnable runnable)
  {
    Preconditions.checkNotNull(runnable, "runnable");

    spigotPlugin.getServer().getScheduler().runTask(spigotPlugin, runnable);
  }

  private void checkForConsumerTimeouts()
  {
    long currentTimeMillis = System.currentTimeMillis();

    responseConsumerMap.removeIf((entry) ->
    {
      ExpirableConsumer<ResponseMessage> responseConsumer = entry.getValue();

      if (responseConsumer.getExpiresAtMillis() > currentTimeMillis)
      {
        // Keep the entry
        return false;
      }

      awaitableExecutor.execute(() ->
      {
        ResponseMessage responseMessage = new ResponseMessage(ResponseStatus.TIMED_OUT);
        responseConsumer.accept(responseMessage);
      });

      // Remove the entry
      return true;
    });
  }

  private void shutdownAwaitableExecutor()
  {
    try
    {
      awaitableExecutor.setAcceptingTasks(false);
      awaitableExecutor.awaitTasksWithSleep(10, 1000);
      awaitableExecutor.shutdown();
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }
  }

  private void buildThreadPoolExecutor()
  {
    ThreadFactoryBuilder factoryBuilder = new ThreadFactoryBuilder();
    factoryBuilder.setNameFormat("SockExchange-Scheduler-Thread-%d");

    ThreadFactory threadFactory = factoryBuilder.build();
    threadPoolExecutor = new ScheduledThreadPoolExecutor(2, threadFactory);

    threadPoolExecutor.setMaximumPoolSize(8);
    threadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    threadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
  }

  private void shutdownThreadPoolExecutor()
  {
    if (!threadPoolExecutor.isShutdown())
    {
      // Disable new tasks from being submitted to service
      threadPoolExecutor.shutdown();

      spigotPlugin.getLogger().info("ScheduledThreadPoolExecutor being shutdown()");

      try
      {
        // Await termination for a minute
        if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS))
        {
          // Force shutdown
          threadPoolExecutor.shutdownNow();

          spigotPlugin.getLogger().severe("ScheduledThreadPoolExecutor being shutdownNow()");

          // Await termination again for another minute
          if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS))
          {
            spigotPlugin.getLogger().severe("ScheduledThreadPoolExecutor not shutdown after shutdownNow()");
          }
        }
      }
      catch (InterruptedException ex)
      {
        spigotPlugin.getLogger().severe("ScheduledThreadPoolExecutor shutdown interrupted");

        // Re-cancel if current thread also interrupted
        threadPoolExecutor.shutdownNow();

        spigotPlugin.getLogger().severe("ScheduledThreadPoolExecutor being shutdownNow()");

        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    }
  }
}
