package us.potatoboy.invview.compat.trinkets;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import us.potatoboy.invview.InvViewPermissions;
import us.potatoboy.invview.PlayerLookup;

public final class TrinketsCompat {
    private TrinketsCompat() {
    }

    public static void register(LiteralCommandNode<CommandSourceStack> root) {
        root.addChild(Commands.literal("trinket")
                .requires(Permissions.require(InvViewPermissions.TRINKET, PermissionLevel.GAMEMASTERS))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(TrinketsCompat::open))
                .build());
    }

    private static int open(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var viewer = source.getPlayerOrException();
        var profile = GameProfileArgument.getGameProfiles(context, "target").iterator().next();
        var server = source.getServer();
        Permissions.check(profile.id(), InvViewPermissions.PROTECTED, false).thenAcceptAsync(protectedPlayer -> {
            if (viewer.hasDisconnected()) {
                return;
            }
            if (protectedPlayer) {
                source.sendFailure(Component.literal("Requested inventory is protected"));
                return;
            }
            var target = PlayerLookup.getPlayer(server, profile);
            var gui = new TrinketsGui(server, viewer, target,
                    Permissions.check(source, InvViewPermissions.MODIFY, true));
            if (gui.prepare() && !gui.open()) {
                source.sendFailure(Component.literal("Could not open trinket inventory"));
            }
        }, server).exceptionally(error -> {
            LogUtils.getLogger().error("Failed to open trinkets for {}", profile.id(), error);
            server.execute(() -> source.sendFailure(Component.literal("Failed to open trinket inventory")));
            return null;
        });
        return 1;
    }
}
