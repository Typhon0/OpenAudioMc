package com.craftmend.openaudiomc.spigot.modules.players.objects;

import com.craftmend.openaudiomc.generic.networking.client.objects.ClientConnection;
import com.craftmend.openaudiomc.spigot.OpenAudioMcSpigot;
import com.craftmend.openaudiomc.generic.media.objects.Media;
import com.craftmend.openaudiomc.spigot.modules.players.handlers.RegionHandler;
import com.craftmend.openaudiomc.spigot.modules.players.handlers.SpeakerHandler;
import com.craftmend.openaudiomc.spigot.modules.players.events.ClientConnectEvent;
import com.craftmend.openaudiomc.spigot.modules.regions.interfaces.IRegion;
import com.craftmend.openaudiomc.spigot.modules.speakers.objects.ApplicableSpeaker;

import com.craftmend.openaudiomc.spigot.modules.speakers.objects.SpeakerSettings;
import com.craftmend.openaudiomc.spigot.services.server.enums.ServerVersion;
import com.craftmend.openaudiomc.spigot.services.utils.DataWatcher;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class SpigotConnection {

    @Getter private ClientConnection clientConnection;

    // optional regions and speakers
    @Setter private List<IRegion> currentRegions = new ArrayList<>();
    @Setter private List<ApplicableSpeaker> currentSpeakers = new ArrayList<>();

    // data watcher that watches for changes in the location, every 5 ticks.
    // If the server version is MODERN (so 1.13 or higher) the task will run sync
    @Getter private DataWatcher<Location> locationDataWatcher = new DataWatcher<>(
            OpenAudioMcSpigot.getInstance(),
            true,
            5
    );

    // Speaker and region handles. Region handler can be null if the feature is disabled
    private SpeakerHandler speakerHandler;
    @Getter private RegionHandler regionHandler;

    //plugin data
    @Setter @Getter private SpeakerSettings selectedSpeakerSettings = null;

    /**
     * @param player client startup logic
     */
    public SpigotConnection(Player player, ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
        // if the region system is enabled, then load the handler
        if (OpenAudioMcSpigot.getInstance().getRegionModule() != null) this.regionHandler = new RegionHandler(player, this);
        // register the speaker handler
        this.speakerHandler = new SpeakerHandler(player, this);

        // code that fires when the location has been changed
        locationDataWatcher.setTask(updatedLocation -> {
            // if the client is not connected, then dont do shit, they wont hear it anyway
            if (!this.clientConnection.getIsConnected()) return;

            // tick the regions, if the regions are enabled
            if (this.regionHandler != null) this.regionHandler.tick();

            // tick the speakers to force them to update
            this.speakerHandler.tick();
        });

        // the feeder, how the data watcher gets its new fed data
        locationDataWatcher.setFeeder(() -> player.getLocation());

        // set handlers
        clientConnection.addOnConnectHandler(() -> {
                    currentRegions.clear();
                    currentSpeakers.clear();
                    Bukkit.getScheduler().runTask(OpenAudioMcSpigot.getInstance(), () -> Bukkit.getServer().getPluginManager().callEvent(new ClientConnectEvent(player, this)));
                });


    }

    /**
     * Called before the Client object is destroyed
     */
    public void onDestroy() {
        // shutdown the data watcher
        this.locationDataWatcher.stop();
    }

    /**
     * @return regions that the player is a part of
     */
    public List<IRegion> getRegions() {
        return currentRegions;
    }

    /**
     * @return speakers in range of the player
     */
    public List<ApplicableSpeaker> getSpeakers() {
        return currentSpeakers;
    }

    /**
     * @param media start media for the client
     */
    public void playMedia(Media media) {
        clientConnection.sendMedia(media);
    }
}
