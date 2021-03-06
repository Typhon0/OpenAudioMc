package com.craftmend.openaudiomc;

import com.craftmend.openaudiomc.generic.authentication.AuthenticationService;
import com.craftmend.openaudiomc.generic.commands.CommandModule;
import com.craftmend.openaudiomc.generic.flags.FlagSet;
import com.craftmend.openaudiomc.generic.interfaces.OAConfiguration;
import com.craftmend.openaudiomc.generic.media.MediaModule;
import com.craftmend.openaudiomc.generic.media.time.TimeService;
import com.craftmend.openaudiomc.generic.networking.interfaces.INetworkingService;
import com.craftmend.openaudiomc.generic.platform.Platform;
import com.craftmend.openaudiomc.generic.objects.OpenAudioApi;
import com.craftmend.openaudiomc.generic.networking.abstracts.AbstractPacketPayload;
import com.craftmend.openaudiomc.generic.networking.addapter.AbstractPacketAdapter;
import com.craftmend.openaudiomc.generic.redis.RedisService;
import com.craftmend.openaudiomc.generic.redis.packets.channels.ChannelKey;
import com.craftmend.openaudiomc.generic.scheduling.interfaces.ITaskProvider;
import com.craftmend.openaudiomc.generic.voice.VoiceRoomManager;
import com.craftmend.openaudiomc.generic.state.StateService;

import com.craftmend.openaudiomc.spigot.OpenAudioMcSpigot;
import com.craftmend.openaudiomc.spigot.modules.configuration.SpigotConfigurationModule;
import com.craftmend.openaudiomc.spigot.modules.show.adapter.RunnableTypeAdapter;
import com.craftmend.openaudiomc.spigot.modules.show.interfaces.ShowRunnable;
import com.craftmend.openaudiomc.spigot.services.scheduling.SpigotTaskProvider;
import com.craftmend.openaudiomc.spigot.services.server.ServerService;

import com.craftmend.openaudiomc.bungee.modules.configuration.BungeeConfigurationModule;
import com.craftmend.openaudiomc.bungee.modules.scheduling.BungeeTaskProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.util.Arrays;

@Getter
public class OpenAudioMc {

    /*
     *  -- NOTE --
     *
     * This is not the plugin of OpenAudioMc, this is the core.
     * For the plugin, see /bungee and the /spigot packages.
     * The core manages all the services used to run OpenAudioMc clients of all types.
     * The core is not version or platform dependant, as long as it runs java.
     *
     * The core is initialized by the plugin (bungeecord or spigot) and only needs a flag
     * to know what platform it is running of.
     *
     * This is always where the magic will happen, since this pretty much handles everything
     * including framework stuff.
     */

    /**
     * Services used by the core to run OpenAudioMc
     * <p>
     *           (SERVICE)                            (PURPOSE)
     * ===========================================================================
     * - State Service           []   (responsible for tracking the current state)
     * - Server Service          []   (used to probe and detect what it is running)
     * - Time Service            []   (used to synchronize time with the central OpenAudioMc-time-server)
     * - Networking Service      []   (handles connections, clients, packets etc)
     * - Flag Set                []   (keeps track of OpenAudioMc account data like if its partnered or not)
     * - Configuration Interface []   (storage implementation for the platform type)
     * - Authentication Service  []   (handle authentication for the api)
     * - Voice Room Manager      []   (keep track of ongoing voice calls)
     * - Command Module          []   (common framework to keep all commands as common-code regardless of platform)
     * - Media Module            []   (keep track of media and timings)
     * - Task Provider           []   (create and register tasks regardless of platform)
     * - Redis Service           []   (provides redis to openaudio and the gang)
     */
    private StateService stateService;
    private ServerService serverService;
    private TimeService timeService;
    private INetworkingService networkingService;
    private FlagSet flagSet;
    private OAConfiguration OAConfiguration;
    private AuthenticationService authenticationService;
    private VoiceRoomManager voiceRoomManager;
    private CommandModule commandModule;
    private MediaModule mediaModule;
    private ITaskProvider taskProvider;
    private RedisService redisService;

    @Getter private static OpenAudioMc instance;

    @Getter private static final OpenAudioApi api = new OpenAudioApi();
    @Getter private static final String server = "http://craftmendserver.eu:81";
    @Getter private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(AbstractPacketPayload.class, new AbstractPacketAdapter())
            .registerTypeAdapter(ShowRunnable.class, new RunnableTypeAdapter())
            .create();

    // The platform, easy for detecting what should be enabled and what not ya know
    private Platform platform;

    public OpenAudioMc(Platform platform, Class<? extends INetworkingService> networkingService) throws Exception {
        instance = this;
        this.platform = platform;

        // if spigot, load the spigot configuration system and the bungee one for bungee
        if (platform == Platform.SPIGOT) {
            this.serverService = new ServerService();
            this.OAConfiguration = new SpigotConfigurationModule(OpenAudioMcSpigot.getInstance());
            this.OAConfiguration.loadSettings();
            this.taskProvider = new SpigotTaskProvider();
        } else {
            this.OAConfiguration = new BungeeConfigurationModule();
            this.OAConfiguration.loadSettings();
            this.taskProvider = new BungeeTaskProvider();
        }

        // enable stuff

        // only enable redis if there are packets that require it for this platform
        if (Arrays.stream(ChannelKey.values()).anyMatch(value -> value.getTargetPlatform() == platform)) {
            this.redisService = new RedisService(this.OAConfiguration);
        }

        this.flagSet = new FlagSet();
        this.authenticationService = new AuthenticationService();
        this.stateService = new StateService();
        this.timeService = new TimeService();
        this.mediaModule = new MediaModule();

        this.networkingService = networkingService.getConstructor().newInstance();

        this.voiceRoomManager = new VoiceRoomManager(this);
        this.commandModule = new CommandModule();
    }

    public void disable() {
        redisService.shutdown();
        OAConfiguration.saveAll();
        if (stateService.getCurrentState().isConnected()) {
            networkingService.stop();
        }
    }
}