package com.originswitcher.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.originswitcher.util.OriginManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Random;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class OriginCommand {

    private static final String DEFAULT_LAYER = "origins:origin";

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess) {
        dispatcher.register(
                literal("os")
                        // /os setorigin <namespace> <path>
                        // e.g. /os setorigin origins phantom
                        // e.g. /os setorigin origins-plus-plus hellforged
                        .then(
                                literal("setorigin")
                                        .then(
                                                argument("namespace", StringArgumentType.word())
                                                        .suggests(namespaceSuggestions())
                                                        .then(
                                                                argument("path", StringArgumentType.word())
                                                                        .suggests(pathSuggestions())
                                                                        .executes(ctx -> executeSetOrigin(ctx,
                                                                                DEFAULT_LAYER,
                                                                                StringArgumentType.getString(ctx, "namespace"),
                                                                                StringArgumentType.getString(ctx, "path")))
                                                        )
                                        )
                        )
                        .then(literal("current").executes(OriginCommand::executeCurrent))
                        .then(
                                literal("list")
                                        .executes(ctx -> executeList(ctx, ""))
                                        .then(
                                                argument("query", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeList(ctx,
                                                                StringArgumentType.getString(ctx, "query")))
                                        )
                        )
                        .then(literal("layers").executes(OriginCommand::executeLayers))
                        .then(literal("random").executes(OriginCommand::executeRandom))
                        .then(
                                literal("persist")
                                        .then(
                                                argument("layerNamespace", StringArgumentType.word())
                                                        .then(
                                                                argument("layerPath", StringArgumentType.word())
                                                                        .then(
                                                                                argument("originNamespace", StringArgumentType.word())
                                                                                        .suggests(namespaceSuggestions())
                                                                                        .then(
                                                                                                argument("originPath", StringArgumentType.word())
                                                                                                        .suggests(pathSuggestions())
                                                                                                        .executes(ctx -> executePersist(ctx,
                                                                                                                StringArgumentType.getString(ctx, "layerNamespace"),
                                                                                                                StringArgumentType.getString(ctx, "layerPath"),
                                                                                                                StringArgumentType.getString(ctx, "originNamespace"),
                                                                                                                StringArgumentType.getString(ctx, "originPath")))
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .executes(OriginCommand::executeHelp)
        );
    }

    private static int executeSetOrigin(CommandContext<FabricClientCommandSource> ctx,
                                        String layerIdStr,
                                        String namespace, String path) {
        if (!OriginManager.isOriginsLoaded()) {
            ctx.getSource().sendError(Text.literal("Origins mod is not loaded!").formatted(Formatting.RED));
            return 0;
        }

        String originIdStr = namespace + ":" + path;
        var player = ctx.getSource().getPlayer();
        Identifier layerId  = new Identifier(layerIdStr);
        Identifier originId = new Identifier(namespace, path);

        boolean success = OriginManager.applyOriginLocally(player, layerId, originId);

        if (success) {
            ctx.getSource().sendFeedback(
                    Text.literal("✓ Origin set to ")
                            .formatted(Formatting.GREEN)
                            .append(Text.literal(originIdStr).formatted(Formatting.AQUA))
            );
            ctx.getSource().sendFeedback(
                    Text.literal("⚠ Client-side only. Use ")
                            .formatted(Formatting.GOLD)
                            .append(
                                    Text.literal("/os persist origins origin " + namespace + " " + path)
                                            .formatted(Formatting.WHITE)
                                            .styled(s -> s.withClickEvent(new ClickEvent(
                                                    ClickEvent.Action.SUGGEST_COMMAND,
                                                    "/os persist origins origin " + namespace + " " + path)))
                            )
                            .append(Text.literal(" (needs OP) to persist.").formatted(Formatting.GOLD))
            );
        } else {
            ctx.getSource().sendError(
                    Text.literal("✗ Failed. Is '" + originIdStr + "' valid? Use /os list to browse.")
                            .formatted(Formatting.RED)
            );
        }
        return success ? 1 : 0;
    }

    private static int executeRandom(CommandContext<FabricClientCommandSource> ctx) {
        if (!OriginManager.isOriginsLoaded()) {
            ctx.getSource().sendError(Text.literal("Origins mod is not loaded!").formatted(Formatting.RED));
            return 0;
        }
        List<Identifier> origins = OriginManager.getAllOriginIds();
        if (origins.isEmpty()) {
            ctx.getSource().sendError(Text.literal("No origins found.").formatted(Formatting.RED));
            return 0;
        }
        Identifier randomOrigin = origins.get(new Random().nextInt(origins.size()));
        return executeSetOrigin(ctx, DEFAULT_LAYER, randomOrigin.getNamespace(), randomOrigin.getPath());
    }

    private static int executeCurrent(CommandContext<FabricClientCommandSource> ctx) {
        if (!OriginManager.isOriginsLoaded()) {
            ctx.getSource().sendError(Text.literal("Origins mod is not loaded!").formatted(Formatting.RED));
            return 0;
        }
        ctx.getSource().sendFeedback(Text.literal("─── Your Current Origins ───").formatted(Formatting.GOLD));
        List<String> layers = OriginManager.getAllLayerIds();
        if (layers.isEmpty()) {
            ctx.getSource().sendFeedback(
                    Text.literal("No layers found.").formatted(Formatting.RED));
            return 0;
        }
        for (String layerId : layers) {
            String originId = OriginManager.getCurrentOriginId(ctx.getSource().getPlayer(), layerId)
                    .orElse("none");
            ctx.getSource().sendFeedback(
                    Text.literal("  Layer: ").formatted(Formatting.GRAY)
                            .append(Text.literal(layerId).formatted(Formatting.YELLOW))
                            .append(Text.literal("  →  ").formatted(Formatting.DARK_GRAY))
                            .append(Text.literal(originId).formatted(Formatting.AQUA))
            );
        }
        return 1;
    }

    private static int executeList(CommandContext<FabricClientCommandSource> ctx, String query) {
        if (!OriginManager.isOriginsLoaded()) {
            ctx.getSource().sendError(Text.literal("Origins mod is not loaded!").formatted(Formatting.RED));
            return 0;
        }
        List<Identifier> origins = query.isEmpty()
                ? OriginManager.getAllOriginIds()
                : OriginManager.searchOrigins(query);

        ctx.getSource().sendFeedback(
                Text.literal("─── Origins" + (query.isEmpty() ? "" : " matching \"" + query + "\"") + " ───")
                        .formatted(Formatting.GOLD));

        if (origins.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("No origins found.").formatted(Formatting.RED));
            return 0;
        }
        for (Identifier id : origins) {
            // Click fills in: /os setorigin <namespace> <path>
            String clickCmd = "/os setorigin " + id.getNamespace() + " " + id.getPath();
            MutableText line = Text.literal("  • ")
                    .formatted(Formatting.DARK_GRAY)
                    .append(
                            Text.literal(id.toString())
                                    .formatted(Formatting.AQUA)
                                    .styled(s -> s
                                            .withClickEvent(new ClickEvent(
                                                    ClickEvent.Action.RUN_COMMAND, clickCmd))
                                            .withHoverEvent(new HoverEvent(
                                                    HoverEvent.Action.SHOW_TEXT,
                                                    Text.literal("Click to become: " + id)
                                                            .formatted(Formatting.GRAY))))
                    );
            ctx.getSource().sendFeedback(line);
        }
        ctx.getSource().sendFeedback(
                Text.literal("(" + origins.size() + " origin(s)) — Click to apply instantly.")
                        .formatted(Formatting.DARK_GRAY));
        return 1;
    }

    private static int executeLayers(CommandContext<FabricClientCommandSource> ctx) {
        if (!OriginManager.isOriginsLoaded()) {
            ctx.getSource().sendError(Text.literal("Origins mod is not loaded!").formatted(Formatting.RED));
            return 0;
        }
        ctx.getSource().sendFeedback(Text.literal("─── Origin Layers ───").formatted(Formatting.GOLD));
        for (String layerId : OriginManager.getAllLayerIds()) {
            ctx.getSource().sendFeedback(
                    Text.literal("  • ").formatted(Formatting.DARK_GRAY)
                            .append(Text.literal(layerId).formatted(Formatting.YELLOW)));
        }
        return 1;
    }

    private static int executePersist(CommandContext<FabricClientCommandSource> ctx,
                                      String layerNamespace, String layerPath,
                                      String originNamespace, String originPath) {
        Identifier layerId  = new Identifier(layerNamespace, layerPath);
        Identifier originId = new Identifier(originNamespace, originPath);
        String cmd = OriginManager.buildDataCommand(layerId, originId);
        ctx.getSource().sendFeedback(
                Text.literal("Sending persistence command (needs OP)...").formatted(Formatting.GRAY));
        OriginManager.sendServerCommand(cmd);
        return 1;
    }

    private static int executeHelp(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal("─── OriginSwitcher Commands ───").formatted(Formatting.GOLD));
        ctx.getSource().sendFeedback(Text.literal("  /os setorigin <namespace> <path>   — Set origin").formatted(Formatting.GRAY));
        ctx.getSource().sendFeedback(Text.literal("    e.g. /os setorigin origins phantom").formatted(Formatting.DARK_GRAY));
        ctx.getSource().sendFeedback(Text.literal("    e.g. /os setorigin origins-plus-plus hellforged").formatted(Formatting.DARK_GRAY));
        ctx.getSource().sendFeedback(Text.literal("  /os random                         — Random origin").formatted(Formatting.GRAY));
        ctx.getSource().sendFeedback(Text.literal("  /os current                        — Show current origins").formatted(Formatting.GRAY));
        ctx.getSource().sendFeedback(Text.literal("  /os list [query]                   — Browse origins (clickable!)").formatted(Formatting.GRAY));
        ctx.getSource().sendFeedback(Text.literal("  /os layers                         — List layers").formatted(Formatting.GRAY));
        ctx.getSource().sendFeedback(Text.literal("  /os persist <lNS> <lPath> <oNS> <oPath> — Persist (needs OP)").formatted(Formatting.GRAY));
        return 1;
    }

    // Suggests namespaces like "origins", "origins-plus-plus"
    private static SuggestionProvider<FabricClientCommandSource> namespaceSuggestions() {
        return (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase();
            OriginManager.getAllOriginIds().stream()
                    .map(Identifier::getNamespace)
                    .distinct()
                    .filter(s -> s.contains(remaining))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    // Suggests paths like "phantom", "hellforged" based on typed namespace
    private static SuggestionProvider<FabricClientCommandSource> pathSuggestions() {
        return (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase();
            // Try to get the namespace already typed
            String namespace;
            try {
                namespace = StringArgumentType.getString(ctx, "namespace");
            } catch (Exception e) {
                namespace = "";
            }
            final String ns = namespace;
            OriginManager.getAllOriginIds().stream()
                    .filter(id -> ns.isEmpty() || id.getNamespace().equals(ns))
                    .map(Identifier::getPath)
                    .filter(s -> s.contains(remaining))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}