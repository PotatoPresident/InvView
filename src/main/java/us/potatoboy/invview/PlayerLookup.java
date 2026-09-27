package us.potatoboy.invview;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import us.potatoboy.invview.mixin.EntityAccessor;

public final class PlayerLookup {
    private PlayerLookup() {
    }

    public static ServerPlayer getPlayer(MinecraftServer server, NameAndId profile) {
        ServerPlayer online = server.getPlayerList().getPlayer(profile.id());
        if (online != null) {
            return online;
        }

        ServerPlayer player = new ServerPlayer(server, server.overworld(),
                new GameProfile(profile.id(), profile.name()), ClientInformation.createDefault());
        try (var reporter = new ProblemReporter.ScopedCollector(LogUtils.getLogger())) {
            server.getPlayerList().loadPlayerData(profile).ifPresent(data -> {
                var input = TagValueInput.create(reporter, server.registryAccess(), data);
                player.load(input);
                input.getString("Dimension").ifPresent(dimension -> {
                    Identifier id = Identifier.tryParse(dimension);
                    if (id != null) {
                        var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                        if (level != null) {
                            ((EntityAccessor) player).callSetLevel(level);
                        }
                    }
                });
            });
        }
        return player;
    }
}
