package us.potatoboy.invview;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class InvView implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            //ViewInvCommand.register(dispatcher);

            LiteralCommandNode<ServerCommandSource> viewNode = CommandManager
                    .literal("view")
                    .build();

            LiteralCommandNode<ServerCommandSource> invNode = CommandManager
                    .literal("inv")
                    .requires((source -> source.hasPermissionLevel(2)))
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ViewCommand::inv))
                    .build();

            LiteralCommandNode<ServerCommandSource> echestNode = CommandManager
                    .literal("echest")
                    .requires((source -> source.hasPermissionLevel(2)))
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ViewCommand::echest))
                    .build();

            dispatcher.getRoot().addChild(viewNode);
            viewNode.addChild(invNode);
            viewNode.addChild(echestNode);
        });

    }
}
