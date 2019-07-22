package com.craftmend.openaudiomc.spigot.modules.commands.subcommands;

import com.craftmend.openaudiomc.spigot.OpenAudioMcSpigot;
import com.craftmend.openaudiomc.spigot.modules.commands.interfaces.SubCommand;
import com.craftmend.openaudiomc.spigot.modules.commands.objects.Argument;
import com.craftmend.openaudiomc.generic.networking.packets.PacketClientDestroyMedia;
import com.craftmend.openaudiomc.spigot.modules.players.objects.SpigotConnection;
import com.craftmend.openaudiomc.spigot.modules.players.objects.PlayerSelector;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StopSubCommand extends SubCommand {

    private OpenAudioMcSpigot openAudioMcSpigot;

    public StopSubCommand(OpenAudioMcSpigot openAudioMcSpigot) {
        super("stop");
        registerArguments(
                new Argument("<selector>",
                        "Stops all manual sounds for all players in a selection"),
                new Argument("<selector> <sound-ID>",
                        "Only stops one specified sound for all players in the selection with a selected ID")
        );
        this.openAudioMcSpigot = openAudioMcSpigot;
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Bukkit.getServer().dispatchCommand(sender, "oa help " + getCommand());
            return;
        }

        if (args.length == 1) {
            for (Player player : new PlayerSelector(args[0]).getPlayers(sender)) {
                SpigotConnection spigotConnection = openAudioMcSpigot.getPlayerModule().getClient(player);
                openAudioMcSpigot.getNetworkingService().send(spigotConnection, new PacketClientDestroyMedia(null));
            }
            message(sender, "Destroyed all normal sounds for the clients in selection");
            return;
        }

        if (args.length == 2) {
            for (Player player : new PlayerSelector(args[0]).getPlayers(sender)) {
                SpigotConnection spigotConnection = openAudioMcSpigot.getPlayerModule().getClient(player);
                openAudioMcSpigot.getNetworkingService().send(spigotConnection, new PacketClientDestroyMedia(args[1]));
            }
            message(sender, "Destroyed all sounds for the clients in selection with id " + args[1]);
            return;
        }

        Bukkit.getServer().dispatchCommand(sender, "oa help " + getCommand());
    }
}