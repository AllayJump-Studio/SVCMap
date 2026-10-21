/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.plugin;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.common.BlueMapConfiguration;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.InterruptableReentrantLock;
import de.bluecolored.bluemap.common.MissingResourcesException;
import de.bluecolored.bluemap.common.addons.AddonLoader;
import de.bluecolored.bluemap.common.api.BlueMapAPIImpl;
import de.bluecolored.bluemap.common.chat.ChatMessage;
import de.bluecolored.bluemap.common.chat.ChatMessageHistory;
import de.bluecolored.bluemap.common.commands.ServerMessages;
import de.bluecolored.bluemap.common.config.*;
import de.bluecolored.bluemap.common.debug.DebugDump;
import de.bluecolored.bluemap.common.debug.StateDumper;
import de.bluecolored.bluemap.common.live.LiveMarkersDataSupplier;
import de.bluecolored.bluemap.common.live.LivePlayersDataSupplier;
import de.bluecolored.bluemap.common.live.PluginLivePlayerInfoTransformer;
import de.bluecolored.bluemap.common.metrics.Metrics;
import de.bluecolored.bluemap.common.plugin.skins.PlayerSkinUpdater;
import de.bluecolored.bluemap.common.rendermanager.MapUpdatePreparationTask;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.common.rendermanager.TileUpdateStrategy;
import de.bluecolored.bluemap.common.rendermanager.serialization.BmMapAdapter;
import de.bluecolored.bluemap.common.rendermanager.serialization.RenderTaskAdapter;
import de.bluecolored.bluemap.common.rendermanager.serialization.Vector2iAdapter;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.common.web.auth.AuthRequestHandler;
import de.bluecolored.bluemap.common.web.auth.WebAccount;
import de.bluecolored.bluemap.common.web.auth.WebAuthGateHandler;
import de.bluecolored.bluemap.common.web.auth.WebAuthService;
import de.bluecolored.bluemap.common.web.auth.WebSession;
import de.bluecolored.bluemap.common.web.auth.WebSessionManager;
import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpHeader;
import de.bluecolored.bluemap.common.web.*;
import de.bluecolored.bluemap.common.web.http.HttpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.map.hires.ArrayTileModel;
import de.bluecolored.bluemap.core.resources.MinecraftVersion;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Tristate;
import de.bluecolored.bluemap.core.util.nbt.LenientListAdapter;
import de.bluecolored.bluemap.core.util.nbt.RegistryAdapter;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.TypeToken;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.*;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Getter
public class Plugin implements ServerEventListener {

    public static final String PLUGIN_ID = "bluemap";
    public static final String PLUGIN_NAME = "BlueMap";

    private static final String DEBUG_FILE_LOG_NAME = "file-debug-log";

    /**
     * Rate-limit for chat messages sent from the webapp: at most N messages per window, and a
     * minimum delay between two messages.
     */
    private static final int WEB_CHAT_MAX_MESSAGES = 5;
    private static final long WEB_CHAT_WINDOW_MILLIS = 30_000L;
    private static final long WEB_CHAT_MIN_INTERVAL_MILLIS = 1_500L;

    @Getter(AccessLevel.NONE)
    private final InterruptableReentrantLock loadingLock = new InterruptableReentrantLock();

    private final String implementationType;
    private final Server serverInterface;

    private BlueMapService blueMap;
    private PluginState pluginState;
    private RenderManager renderManager;
    private BlueMapAPIImpl api;

    private HttpServer webServer;
    private RoutingRequestHandler webRequestHandler;
    private Logger webLogger;

    /**
     * All map-request-handlers that currently serve SSE connections.
     * Chat is server-wide, so a chat message is broadcast to all of them (and thus to the
     * client that is currently viewing any map).
     */
    @DebugDump(exclude = true)
    private final List<MapRequestHandler> mapRequestHandlers = new CopyOnWriteArrayList<>();

    @DebugDump(exclude = true)
    private volatile @Nullable ChatMessageHistory chatHistory = null;

    @DebugDump(exclude = true)
    private volatile boolean chatEnabled = false;

    @DebugDump(exclude = true)
    private volatile int chatMessageLengthLimit = 256;

    @DebugDump(exclude = true)
    private volatile boolean chatUseVanillaChatEvent = true;

    @DebugDump(exclude = true)
    private volatile String chatIngamePrefix = "[WEB] ";

    /**
     * Web-account / login system, or {@code null} if it is disabled in the webserver-config
     * (or if the account file could not be loaded).
     */
    private @Nullable WebAuthService webAuth = null;

    /**
     * Session storage for the web-login, or {@code null} if the login system is disabled.
     */
    private @Nullable WebSessionManager webSessions = null;

    /**
     * Recent webapp chat timestamps per account, for rate-limiting.
     */
    private final Map<String, Deque<Long>> webChatTimestamps = new ConcurrentHashMap<>();

    /**
     * Localized texts for the in-game commands (overridable in {@code <configFolder>/lang/}).
     */
    private volatile ServerMessages serverMessages = ServerMessages.bundledOnly();

    private Timer timer;
    private Map<String, MapUpdateService> mapUpdateServices;
    private PlayerSkinUpdater skinUpdater;
    private PluginLivePlayerInfoTransformer livePlayerInfoTransformer;

    private boolean loaded = false;

    public Plugin(String implementationType, Server serverInterface) {
        this.implementationType = implementationType.toLowerCase();
        this.serverInterface = serverInterface;

        StateDumper.global().register(this);
    }

    public void load() throws IOException {
        load(null);
    }

    private void load(@Nullable ResourcePack preloadedResourcePack) throws IOException {
        loadingLock.lock();
        try {
            synchronized (this) {

                if (loaded) return;
                unload(); //ensure nothing is left running (from a failed load or something)

                //load addons
                Path packsFolder = serverInterface.getConfigFolder().resolve("packs");
                Files.createDirectories(packsFolder);
                AddonLoader.INSTANCE.tryLoadAddons(packsFolder);

                //load configs
                BlueMapConfigManager configManager = BlueMapConfigManager.builder()
                        .minecraftVersion(serverInterface.getMinecraftVersion())
                        .configRoot(serverInterface.getConfigFolder())
                        .packsFolder(packsFolder)
                        .modsFolder(serverInterface.getModsFolder().orElse(null))
                        .defaultBlockStates(serverInterface.getDefaultBlockstates())
                        .useMetricsConfig(serverInterface.isMetricsEnabled() == Tristate.UNDEFINED)
                        .autoConfigWorlds(serverInterface.getLoadedServerWorlds())
                        .build();
                CoreConfig coreConfig = configManager.getCoreConfig();
                WebserverConfig webserverConfig = configManager.getWebserverConfig();
                WebappConfig webappConfig = configManager.getWebappConfig();
                PluginConfig pluginConfig = configManager.getPluginConfig();

                //apply chat config
                PluginConfig.ChatConfig chatConfig = pluginConfig.getChat();
                this.chatEnabled = chatConfig.isEnabled();
                this.chatMessageLengthLimit = chatConfig.getMessageLengthLimit();
                this.chatUseVanillaChatEvent = chatConfig.isUseVanillaChatEvent();
                this.chatIngamePrefix = chatConfig.getIngamePrefix() == null ? "" : chatConfig.getIngamePrefix();
                this.chatHistory = this.chatEnabled ? new ChatMessageHistory(chatConfig.getHistoryLength()) : null;

                //init web-accounts
                this.webAuth = null;
                this.webSessions = null;
                WebserverConfig.AuthConfig authConfig = webserverConfig.getAuth();
                Path authConfigFolder = serverInterface.getConfigFolder();

                // localized command-texts, admins can override them in <configFolder>/lang/
                this.serverMessages = ServerMessages.load(authConfigFolder);

                // The account store is always loaded, so "/bluemap webregister" works BEFORE the
                // login is enabled. Otherwise the documented order (register first, then enable)
                // would be impossible. "enabled" only controls the gate and the login endpoints.
                Path authFile = authConfigFolder.resolve("webauth.json");
                WebAuthService webAuth = new WebAuthService(
                        authFile,
                        authConfig.getPasscodeExpiryMinutes(),
                        authConfig.getMinPasswordLength()
                );
                try {
                    webAuth.load();
                    this.webAuth = webAuth;
                    Logger.global.logDebug("Loaded %d web-account(s) from %s".formatted(
                            webAuth.getAccountNames().size(), authFile));
                } catch (IOException | RuntimeException ex) {
                    // fail closed: without accounts nobody can log in, so no map data is served either
                    Logger.global.logError("Failed to load the web-accounts from " + authFile +
                            " - all map data is denied until this is fixed!", ex);
                }

                if (authConfig.isEnabled()) {
                    // sessions first, so the gate can fail closed even if the account file is broken
                    WebSessionManager sessionManager = new WebSessionManager(
                            authConfigFolder.resolve("sessions.json"),
                            authConfig.getSessionDays(),
                            authConfig.getRememberDays(),
                            authConfig.getIdleTimeoutMinutes()
                    );
                    try {
                        sessionManager.load();
                    } catch (IOException | RuntimeException ex) {
                        Logger.global.logError("Failed to load sessions.json - everyone has to log in again!", ex);
                    }
                    this.webSessions = sessionManager;
                }

                //apply new file-logger config
                if (coreConfig.getLog().getFile() != null) {
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
                    Logger.global.put(DEBUG_FILE_LOG_NAME, () -> Logger.file(
                            Path.of(String.format(coreConfig.getLog().getFile(), zdt)),
                            coreConfig.getLog().isAppend()
                    ));
                } else {
                    Logger.global.remove(DEBUG_FILE_LOG_NAME);
                }

                Logger.global.logDebug("Loading BlueMap v%s (%s) on %s %s ...".formatted(
                        BlueMap.VERSION,
                        BlueMap.GIT_HASH,
                        implementationType,
                        serverInterface.getMinecraftVersion()
                ));

                //load plugin state
                try {
                    GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                            .path(coreConfig.getData().resolve("pluginState.json"))
                            .build();
                    pluginState = loader.load().get(PluginState.class);
                } catch (SerializationException ex) {
                    Logger.global.logWarning("Failed to load pluginState.json (invalid format), creating a new one...");
                    pluginState = new PluginState();
                }

                //create bluemap-service
                blueMap = new BlueMapService(configManager, preloadedResourcePack);

                //try load resources
                try {
                    blueMap.getOrLoadResourcePack();
                } catch (MissingResourcesException ex) {
                    Logger.global.logWarning("BlueMap is missing important resources!");
                    Logger.global.logWarning("You must accept the required file download in order for BlueMap to work!");

                    BlueMapConfiguration configProvider = blueMap.getConfig();
                    if (configProvider instanceof BlueMapConfigManager) {
                        Logger.global.logWarning("Please check: " + ((BlueMapConfigManager) configProvider).getConfigManager().resolveConfigFile(BlueMapConfigManager.CORE_CONFIG_NAME).toAbsolutePath().normalize());
                    }

                    Logger.global.logInfo("If you have changed the config you can simply reload the plugin using: /bluemap reload");

                    unload();
                    return;
                }

                //load maps
                Map<String, BmMap> maps = blueMap.getOrLoadMaps();

                //init live data suppliers
                livePlayerInfoTransformer = new PluginLivePlayerInfoTransformer(this);

                //create and start webserver
                if (webserverConfig.isEnabled()) {
                    Path webroot = webserverConfig.getWebroot();
                    FileHelper.createDirectories(webroot);

                    this.webRequestHandler = new RoutingRequestHandler();

                    // default route
                    webRequestHandler.register(".*", new FileRequestHandler(webroot));

                    // map route
                    for (var mapConfigEntry : configManager.getMapConfigs().entrySet()) {
                        String id = mapConfigEntry.getKey();
                        MapConfig mapConfig = mapConfigEntry.getValue();

                        MapRequestHandler mapRequestHandler;
                        BmMap map = maps.get(id);
                        if (map != null) {
                            LivePlayersDataSupplier livePlayersDataSupplier = pluginConfig.isLivePlayerMarkers() ?
                                    new LivePlayersDataSupplier(serverInterface, map.getWorld(), livePlayerInfoTransformer, pluginConfig.isHideDifferentWorld()) :
                                    null;
                            LiveMarkersDataSupplier liveMarkersDataSupplier = new LiveMarkersDataSupplier(map.getMarkerSets());

                            mapRequestHandler = new MapRequestHandler(
                                    map,
                                    livePlayersDataSupplier,
                                    liveMarkersDataSupplier,
                                    this.chatHistory,
                                    webserverConfig.isSseEnabled()
                            );
                        } else {
                            Storage storage = blueMap.getOrLoadStorage(mapConfig.getStorage());
                            mapRequestHandler = new MapRequestHandler(storage.map(id), null, null, this.chatHistory, false);
                        }

                        // chat is broadcast to all maps, so we need to keep track of the handlers
                        if (this.chatHistory != null) this.mapRequestHandlers.add(mapRequestHandler);

                        // idle sessions must not receive any more map data over SSE
                        if (this.webSessions != null) mapRequestHandler.setSessionResolver(this::resolveWebSession);

                        // webapp chat uplink (needs the login system, so the identity is trusted)
                        if (this.chatEnabled && this.webAuth != null) mapRequestHandler.setChatSubmitter(this::submitWebChat);

                        webRequestHandler.register(
                                "maps/" + Pattern.quote(id) + "/(.*)",
                                "$1",
                                mapRequestHandler
                        );
                    }

                    // create web-logger
                    List<Logger> webLoggerList = new ArrayList<>();
                    if (webserverConfig.getLog().getFile() != null) {
                        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
                        webLoggerList.add(Logger.file(
                                Path.of(String.format(webserverConfig.getLog().getFile(), zdt)),
                                webserverConfig.getLog().isAppend()
                        ));
                    }
                    webLogger = Logger.combine(webLoggerList);

                    // web-login endpoints (registered after the catch-all, so they take precedence)
                    WebSessionManager sessionManager = this.webSessions;
                    if (this.webAuth != null && sessionManager != null) {
                        webRequestHandler.register(
                                "api/auth/(.*)",
                                "$1",
                                new AuthRequestHandler(this.webAuth, sessionManager)
                        );
                    }

                    // gate everything that is not part of the login flow
                    HttpRequestHandler gatedHandler = webRequestHandler;
                    if (this.webSessions != null && webserverConfig.getAuth().isRequireLogin()) {
                        gatedHandler = new WebAuthGateHandler(webRequestHandler, this.webSessions, true);
                        if (this.webAuth != null) {
                            Logger.global.logInfo("The webserver now requires a login for all map data.");
                        } else {
                            Logger.global.logWarning("Web-accounts could not be loaded - denying all map data " +
                                    "(fix webauth.json and run /bluemap reload)!");
                        }
                    }

                    try {
                        webServer = new HttpServer(
                                "BlueMap-Webserver",
                                new LoggingRequestHandler(
                                        new BlueMapResponseModifier(gatedHandler, webserverConfig.getAdditionalHeaders()),
                                        webserverConfig.getLog().getFormat(),
                                        webLogger
                                )
                        );
                        webServer.bind(new InetSocketAddress(
                                webserverConfig.resolveIp(),
                                webserverConfig.getPort()
                        ));
                        webServer.start();
                    } catch (UnknownHostException ex) {
                        throw new ConfigurationException("BlueMap failed to resolve the ip in your webserver-config.\n" +
                                "Check if that is correctly configured.", ex);
                    } catch (BindException ex) {
                        throw new ConfigurationException("BlueMap failed to bind to the configured address.\n" +
                                "This usually happens when the configured port (" + webserverConfig.getPort() + ") is already in use by some other program.", ex);
                    } catch (IOException ex) {
                        throw new ConfigurationException("""
                                BlueMap failed to initialize the webserver.
                                Check your webserver-config if everything is configured correctly.
                                (Make sure you DON'T use the same port for bluemap that you also use for your minecraft server)
                                """.strip(), ex);
                    }
                }

                //warn if no maps are configured
                if (maps.isEmpty()) {
                    Logger.global.logWarning("There are no valid maps configured, please check your map-configs! Disabling BlueMap...");
                    unload(true);
                    return;
                }

                //initialize render manager
                renderManager = new RenderManager();

                //update webapp and settings
                if (webappConfig.isEnabled())
                    blueMap.createOrUpdateWebApp(false);

                //start skin updater
                this.skinUpdater = new PlayerSkinUpdater(this);
                if (pluginConfig.isSkinDownload()) {
                    serverInterface.registerListener(skinUpdater);
                }

                //init timer
                timer = new Timer("BlueMap-Plugin-Timer", true);

                //periodically save and release memory
                TimerTask saveTask = new TimerTask() {
                    @Override
                    public void run() {
                        save();

                        // if nothing is being actively rendered, clear caches and pools to release some heap-space
                        if (!renderManager.isRunning() || renderManager.getCurrentRenderTask() == null)
                            ArrayTileModel.instancePool().clear();
                    }
                };
                timer.schedule(saveTask, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10));

                //periodically save markers
                int writeMarkersInterval = pluginConfig.getWriteMarkersInterval();
                if (writeMarkersInterval > 0) {
                    TimerTask saveMarkersTask = new TimerTask() {
                        @Override
                        public void run() {
                            saveMarkerStates();
                        }
                    };
                    timer.schedule(saveMarkersTask, TimeUnit.SECONDS.toMillis(writeMarkersInterval), TimeUnit.SECONDS.toMillis(writeMarkersInterval));
                }

                //periodically save players
                int writePlayersInterval = pluginConfig.getWritePlayersInterval();
                if (writePlayersInterval > 0) {
                    TimerTask savePlayersTask = new TimerTask() {
                        @Override
                        public void run() {
                            savePlayerStates();
                        }
                    };
                    timer.schedule(savePlayersTask, TimeUnit.SECONDS.toMillis(writePlayersInterval), TimeUnit.SECONDS.toMillis(writePlayersInterval));
                }

                //periodically restart the file-watchers
                TimerTask fileWatcherRestartTask = new TimerTask() {
                    @Override
                    public void run() {
                        mapUpdateServices.values().forEach(MapUpdateService::close);
                        mapUpdateServices.clear();
                        initMapUpdateTasks();
                    }
                };
                timer.schedule(fileWatcherRestartTask, TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1));

                //update all (non frozen) maps
                Duration fullUpdateInterval = coreConfig.getFullUpdateInterval();
                if (fullUpdateInterval.isPositive()) {
                    renderManager.scheduleRenderTasksNext(maps.values().stream()
                            .filter(map -> pluginState.getMapState(map).isUpdateEnabled())
                            .filter(map -> {
                                Instant nextUpdate = pluginState.getMapState(map).getLastFullUpdate().plus(fullUpdateInterval);
                                Duration delay = Instant.now().until(nextUpdate);
                                return !delay.isPositive();
                            })
                            .sorted(Comparator.comparing(bmMap -> bmMap.getMapSettings().getSorting()))
                            .peek(map -> pluginState.getMapState(map).setLastFullUpdate(Instant.now()))
                            .map(map -> MapUpdatePreparationTask.updateMap(map, renderManager))
                            .toArray(RenderTask[]::new));
                }

                // load render-tasks
                Path tasksFile = blueMap.getConfig().getCoreConfig().getData().resolve("tasks.dat");
                if (Files.exists(tasksFile)) {
                    try (InputStream in = Files.newInputStream(tasksFile)) {
                        BlueNBT blueNBT = createRenderTaskBlueNBT();
                        TasksData tasksData = blueNBT.read(in, new TypeToken<>() {});
                        renderManager.scheduleRenderTasks(tasksData.getRenderTasks().stream()
                                .filter(Objects::nonNull)
                                .toArray(RenderTask[]::new)
                        );
                    } catch (Exception ex) {
                        Logger.global.logError("Failed to load tasks.dat!", ex);
                        Files.delete(tasksFile);
                    }
                }

                //metrics
                MinecraftVersion minecraftVersion = blueMap.getOrLoadMinecraftVersion();
                TimerTask metricsTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (serverInterface.isMetricsEnabled().getOr(coreConfig::isMetrics))
                            Metrics.sendReport(implementationType, minecraftVersion.getId());
                    }
                };
                timer.scheduleAtFixedRate(metricsTask, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(30));

                //watch map-changes
                this.mapUpdateServices = new HashMap<>();
                initMapUpdateTasks();

                //register listener
                serverInterface.registerListener(this);

                //enable api
                this.api = new BlueMapAPIImpl(this);
                this.api.register();

                //start render-manager
                if (pluginState.isRenderThreadsEnabled()) {
                    checkPausedByPlayerCount(); // <- this also starts the render-manager if it should start
                } else {
                    Logger.global.logInfo("Render-Threads are STOPPED! Use the command 'bluemap start' to start them.");
                }

                //done
                loaded = true;
            }
        } catch (ConfigurationException ex) {
            Logger.global.logWarning(ex.getFormattedExplanation());
            throw new IOException(ex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.global.logWarning("Loading has been interrupted!");
        } finally {
            loadingLock.unlock();
        }
    }

    public void unload() {
        this.unload(false);
    }

    public void unload(boolean keepWebserver) {
        loadingLock.interruptAndLock();
        try {
            synchronized (this) {

                //disable api
                if (api != null) api.unregister();
                api = null;

                //unregister listeners
                serverInterface.unregisterAllListeners();
                skinUpdater = null;

                //stop scheduled threads
                if (timer != null) timer.cancel();
                timer = null;

                //stop file-watchers
                if (mapUpdateServices != null) {
                    mapUpdateServices.values().forEach(MapUpdateService::close);
                    mapUpdateServices.clear();
                }
                mapUpdateServices = null;

                //save
                save();

                // specifically save empty (disabled) players to the loaded maps
                if (blueMap != null) {
                    for (BmMap map : blueMap.getMaps().values()) {
                        map.savePlayerState();
                    }
                }

                // stop render-manager
                if (renderManager != null){
                    if (renderManager.getCurrentRenderTask() != null) {
                        renderManager.removeAllRenderTasks();
                        if (!renderManager.isRunning()) renderManager.start(1, Thread.NORM_PRIORITY);
                        try {
                            renderManager.awaitIdle(true);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    renderManager.stop();
                    try {
                        renderManager.awaitShutdown();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                // stop webserver
                if (!keepWebserver) {
                    if (webServer != null) {
                        try {
                            webServer.close();
                        } catch (IOException ex) {
                            Logger.global.logError("Failed to close the webserver!", ex);
                        }
                        webServer = null;
                    }

                    if (webRequestHandler != null) {
                        webRequestHandler.close();
                        webRequestHandler = null;
                    }

                    if (webLogger != null) {
                        try {
                            webLogger.close();
                        } catch (Exception ex) {
                            Logger.global.logError("Failed to close the webserver-logger!", ex);
                        }
                        webLogger = null;
                    }
                }

                //close bluemap
                if (blueMap != null) {
                    try {
                        blueMap.close();
                    } catch (IOException ex) {
                        Logger.global.logError("Failed to close a bluemap-service!", ex);
                    }
                }
                blueMap = null;

                // remove file-logger
                Logger.global.remove(DEBUG_FILE_LOG_NAME);

                //clear resources
                pluginState = null;

                //stop forwarding chat
                chatEnabled = false;
                chatHistory = null;
                mapRequestHandlers.clear();

                //drop the web-account system
                webAuth = null;
                webSessions = null;
                webChatTimestamps.clear();

                //done
                loaded = false;
            }
        } finally {
            loadingLock.unlock();
        }
    }

    public void reload() throws IOException {
        unload();
        load();
    }

    /**
     * {@link #reload()} but without reloading the resourcepack (if it is loaded).
     */
    public void lightReload() throws IOException {
        loadingLock.lock();
        try {
            synchronized (this) {

                if (!loaded) {
                    reload(); // reload normally
                    return;
                }

                // hold and reuse loaded resourcepack
                ResourcePack preloadedResourcePack = this.blueMap.getResourcePack();

                unload();
                load(preloadedResourcePack);

            }
        } finally {
            loadingLock.unlock();
        }
    }

    public synchronized void save() {
        if (blueMap == null) return;

        if (renderManager != null) {
            BlueNBT blueNBT = createRenderTaskBlueNBT();
            Path file = blueMap.getConfig().getCoreConfig().getData().resolve("tasks.dat");
            TasksData tasksData = new TasksData();
            tasksData.setRenderTasks(renderManager.getScheduledRenderTasks());
            try (OutputStream out = FileHelper.createFilepartOutputStream(file)) {
                blueNBT.write(tasksData, out, new TypeToken<>() {});
            } catch (Exception ex) {
                Logger.global.logError("Failed to save tasks.dat!", ex);
            }
        }

        if (pluginState != null) {
            try {
                GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                        .path(blueMap.getConfig().getCoreConfig().getData().resolve("pluginState.json"))
                        .indent(0)
                        .build();
                loader.save(loader.createNode().set(PluginState.class, pluginState));
            } catch (Exception ex) {
                Logger.global.logError("Failed to save pluginState.json!", ex);
            }
        }

        var maps = blueMap.getMaps();
        for (BmMap map : maps.values()) {
            map.save();
        }
    }

    private BlueNBT createRenderTaskBlueNBT() {
        BlueNBT blueNBT = new BlueNBT();
        blueNBT.register(TypeToken.of(TileUpdateStrategy.class), new RegistryAdapter<>(TileUpdateStrategy.REGISTRY, Key.BLUEMAP_NAMESPACE, TileUpdateStrategy.FORCE_NONE));
        blueNBT.register(TypeToken.of(Vector2i.class), new Vector2iAdapter());
        blueNBT.register(TypeToken.of(BmMap.class), new BmMapAdapter(blueMap));

        // a bit of trickery to allow the RenderTaskAdapter to use itself recursively through BlueNBT's default serialization
        RenderTaskAdapter renderTaskAdapter = new RenderTaskAdapter();
        blueNBT.register(TypeToken.of(RenderTask.class), renderTaskAdapter);
        blueNBT.register(
                new TypeToken<>() {},
                new LenientListAdapter<>(blueNBT, TypeToken.of(RenderTask.class), e -> Logger.global.logDebug("Failed to load render-task: " + e))
        );
        renderTaskAdapter.init(blueNBT);

        return blueNBT;
    }

    public void saveMarkerStates() {
        if (blueMap == null) return;

        var maps = blueMap.getMaps();
        for (BmMap map : maps.values()) {
            map.saveMarkerState();
        }
    }

    public void savePlayerStates() {
        if (blueMap == null) return;

        var maps = blueMap.getMaps();
        for (BmMap map : maps.values()) {
            var dataSupplier = new LivePlayersDataSupplier(
                    serverInterface,
                    map.getWorld(),
                    livePlayerInfoTransformer,
                    blueMap.getConfig().getPluginConfig().isHideDifferentWorld()
            );
            try (
                    OutputStream out = map.getStorage().players().write();
                    Writer writer = new OutputStreamWriter(out)
            ) {
                writer.write(dataSupplier.get());
            } catch (Exception ex) {
                Logger.global.logError("Failed to save players for map '" + map.getId() + "'!", ex);
            }
        }
    }

    public synchronized void startWatchingMap(BmMap map) {
        stopWatchingMap(map);

        if (blueMap == null) return;

        try {
            MapUpdateService watcher = MapUpdateService.builder()
                    .renderManager(renderManager)
                    .map(map)
                    .lastFullUpdate(pluginState.getMapState(map).getLastFullUpdate())
                    .fullUpdateInterval(blueMap.getConfig().getCoreConfig().getFullUpdateInterval())
                    .regionUpdateCooldown(blueMap.getConfig().getCoreConfig().getUpdateCooldown())
                    .regionCheckInterval(blueMap.getConfig().getCoreConfig().getRegionFileCheckInterval())
                    .onFullUpdate(instant -> pluginState.getMapState(map).setLastFullUpdate(instant))
                    .build();
            watcher.start();
            mapUpdateServices.put(map.getId(), watcher);
        } catch (IOException ex) {
            Logger.global.logError("Failed to create update-watcher for map: " + map.getId() +
                    " (This means the map might not automatically update)", ex);
        } catch (UnsupportedOperationException ex) {
            Logger.global.logWarning("Update-watcher for map '" + map.getId() + "' is not supported for the world-type." +
                    " (This means the map might not automatically update)");
        }
    }

    public synchronized void stopWatchingMap(BmMap map) {
        MapUpdateService watcher = mapUpdateServices.remove(map.getId());
        if (watcher != null) {
            watcher.close();
        }
    }

    public boolean flushWorldUpdates(World world) throws IOException {
        var implWorld = serverInterface.getServerWorld(world).orElse(null);
        if (implWorld != null) return implWorld.persistWorldChanges();
        return false;
    }

    @Override
    public void onPlayerJoin(UUID playerUuid) {
        checkPausedByPlayerCountSoon();
    }

    @Override
    public void onPlayerLeave(UUID playerUuid) {
        checkPausedByPlayerCountSoon();
    }

    /**
     * Resolves the web session of a request, or {@code null} if there is none.
     */
    private @Nullable WebSession resolveWebSession(HttpRequest request) {
        WebSessionManager sessions = this.webSessions;
        if (sessions == null) return null;

        HttpHeader cookieHeader = request.getHeader("cookie");
        if (cookieHeader == null) return null;

        List<String> values = cookieHeader.getValues();
        if (values.isEmpty()) return null;

        return sessions.resolveCookieHeader(String.join("; ", values));
    }

    @Override
    public void onPlayerChat(UUID playerUuid, String playerName, String message) {
        if (!chatEnabled) return;
        if (message == null || message.isEmpty()) return;

        ChatMessage chatMessage = ChatMessage.player(playerUuid, playerName, truncateChatMessage(message));

        // keep the recent chat, so a (re)connecting client can load it via live/chat.json
        ChatMessageHistory history = this.chatHistory;
        if (history != null) history.add(chatMessage);

        // broadcast to every map, so it reaches the client that is currently viewing any of them
        for (MapRequestHandler mapRequestHandler : mapRequestHandlers) {
            mapRequestHandler.broadcastChat(chatMessage);
        }
    }

    private String truncateChatMessage(String message) {
        int limit = chatMessageLengthLimit;
        if (limit <= 0 || message.length() <= limit) return message;
        return message.substring(0, limit);
    }

    /**
     * Delivers a chat message from a logged-in webapp-user into the game and to all webapp clients.
     *
     * @param accountName the account name of the sender, taken from the session
     * @param message the raw message text
     * @return {@code null} on success, or an error code for the webapp
     */
    private @Nullable String submitWebChat(String accountName, String message) {
        if (!chatEnabled) return "chat-disabled";

        // the identity is the web-account, so the login system has to be the source of truth
        WebAuthService webAuth = this.webAuth;
        if (webAuth == null) return "login-required";

        String text = sanitizeWebChatMessage(message);
        if (text.isEmpty()) return "empty";

        int limit = chatMessageLengthLimit;
        if (limit > 0 && text.length() > limit) return "too-long";

        if (!allowWebChat(accountName)) return "rate-limited";

        WebAccount account = webAuth.getAccount(accountName);
        UUID playerUuid = account != null ? account.getPlayerUuid() : null;

        // permission gate (bluemap.webchat), checked for the account's player
        if (playerUuid == null || !serverInterface.hasPermission(playerUuid, "bluemap.webchat")) {
            return "no-permission";
        }

        // deliver into the game:
        // preferred: as the player, so the normal chat event runs (filters, mutes, other plugins)
        // fallback: plain broadcast, if the player is offline or the chat event is disabled in the config
        String prefix = chatIngamePrefix;
        boolean viaChatEvent = chatUseVanillaChatEvent && serverInterface.sendChatAsPlayer(playerUuid, prefix + text);

        if (!viaChatEvent) {
            Component gameMessage = Component.text()
                    .append(Component.text(prefix, TextColor.color(0x88ccff)))
                    .append(Component.text(accountName + ": ", TextColor.color(0xffffff)))
                    .append(Component.text(text, TextColor.color(0xdddddd)))
                    .build();
            serverInterface.broadcastMessage(gameMessage);

            // the chat event is not involved here, so nothing else would log this message
            Logger.global.logInfo("[Web] %s: %s".formatted(accountName, text));
        }

        ChatMessage chatMessage = ChatMessage.web(playerUuid, accountName, text);

        // keep the recent chat, so a (re)connecting client can load it via live/chat.json
        ChatMessageHistory history = this.chatHistory;
        if (history != null) history.add(chatMessage);

        // to every map, so it reaches the client that is currently viewing any of them
        for (MapRequestHandler mapRequestHandler : mapRequestHandlers) {
            mapRequestHandler.broadcastChat(chatMessage);
        }

        return null;
    }

    /**
     * Removes color-codes and control characters, so a webapp message cannot break the chat format.
     */
    private static String sanitizeWebChatMessage(@Nullable String message) {
        if (message == null) return "";

        StringBuilder builder = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§') { // skip the color-code character and the code itself
                i++;
                continue;
            }
            if (c < 0x20 || c == 0x7f) continue; // newlines, tabs, ...
            builder.append(c);
        }
        return builder.toString().trim();
    }

    /**
     * Simple per-account rate-limit for webapp chat messages.
     */
    private boolean allowWebChat(String accountName) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = webChatTimestamps.computeIfAbsent(accountName, key -> new ArrayDeque<>());

        synchronized (timestamps) {
            Long last = timestamps.peekLast();
            if (last != null && now - last < WEB_CHAT_MIN_INTERVAL_MILLIS) return false;

            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WEB_CHAT_WINDOW_MILLIS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= WEB_CHAT_MAX_MESSAGES) return false;

            timestamps.addLast(now);
            return true;
        }
    }

    private void checkPausedByPlayerCountSoon() {
        // check is done a second later to make sure the player has actually joined/left and is no longer on the list
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    checkPausedByPlayerCount();
                }
            }, 1000);
        } catch (IllegalStateException ex) { // Timer is cancelled for some reason
            Logger.global.logWarning("Timer is already cancelled, skipping player-limit checks!");
        }
    }

    public boolean checkPausedByPlayerCount() {
        CoreConfig coreConfig = getBlueMap().getConfig().getCoreConfig();
        PluginConfig pluginConfig = getBlueMap().getConfig().getPluginConfig();

        if (
                pluginConfig.getPlayerRenderLimit() > 0 &&
                getServerInterface().getOnlinePlayers().size() >= pluginConfig.getPlayerRenderLimit()
        ) {
            if (renderManager.isRunning()) renderManager.stop();
            return true;
        } else {
            if (!renderManager.isRunning() && getPluginState().isRenderThreadsEnabled())
                renderManager.start(coreConfig.resolveRenderThreadCount(), coreConfig.getRenderThreadPriority());
            return false;
        }
    }

    public @Nullable World getWorld(ServerWorld serverWorld) {
        String id = World.id(serverWorld.getWorldFolder(), serverWorld.getDimension());
        return getBlueMap().getWorlds().get(id);
    }

    private void initMapUpdateTasks() {
        var maps = blueMap.getMaps();
        if (maps != null) {
            for (BmMap map : maps.values()) {
                if (pluginState.getMapState(map).isUpdateEnabled()) {
                    startWatchingMap(map);
                }
            }
        }
    }

    public boolean isLoading() {
        return loadingLock.isLocked();
    }

}
