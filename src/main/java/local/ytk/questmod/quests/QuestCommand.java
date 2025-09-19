package local.ytk.questmod.quests;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.command.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.*;
import static com.mojang.brigadier.arguments.BoolArgumentType.*;
import static com.mojang.brigadier.arguments.IntegerArgumentType.*;
import static local.ytk.questmod.QuestMod.*;
import static local.ytk.questmod.Util.*;
import static net.minecraft.command.argument.EntityArgumentType.*;
import static net.minecraft.server.command.CommandManager.*;

public class QuestCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("quest")
                .then(literal("create")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(QuestCommand::create))
                .then(literal("list").executes(QuestCommand::get))
                .then(literal("get")
                        .executes(QuestCommand::get)
                        .then(argument("id", integer(0))
                                .suggests(QuestCommand::getIdSuggestions)
                                .executes(QuestCommand::get)))
                .then(literal("check")
                        .executes(QuestCommand::check)
                        .then(argument("player", player())
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(QuestCommand::check)
                                .then(literal("*")
                                        .executes(QuestCommand::check))
                                .then(argument("id", integer(0))
                                        .suggests(QuestCommand::getIdSuggestions)
                                        .executes(QuestCommand::check)))
                        .then(literal("*")
                                .executes(QuestCommand::check))
                        .then(argument("id", integer(0))
                                .suggests(QuestCommand::getIdSuggestions)
                                .executes(QuestCommand::check)))
                .then(literal("claim")
                        .executes(QuestCommand::claim)
                        .then(argument("player", player())
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(QuestCommand::claim)
                                .then(literal("*")
                                        .executes(QuestCommand::claim)
                                        .then(argument("check", bool())
                                                .executes(QuestCommand::claim)))
                                .then(argument("id", integer(0))
                                        .suggests(QuestCommand::getIdSuggestions)
                                        .executes(QuestCommand::claim)
                                        .then(argument("check", bool())
                                                .executes(QuestCommand::claim))))
                        .then(literal("*")
                                .executes(QuestCommand::claim)
                                .then(argument("check", bool())
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(QuestCommand::claim)))
                        .then(argument("id", integer(0))
                                .suggests(QuestCommand::getIdSuggestions)
                                .executes(QuestCommand::claim)
                                .then(argument("check", bool())
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(QuestCommand::claim))))
                .then(literal("remove")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("id", integer(0))
                                .suggests(QuestCommand::getIdSuggestions)
                                .executes(QuestCommand::remove)))
                .then(literal("clear")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(QuestCommand::clear))
                .then(literal("menu")
                        .executes(QuestCommand::menu)));
    }
    
    private static int create(CommandContext<ServerCommandSource> context) {
        boolean success = createQuest(context.getSource().getServer());
        return success ? SINGLE_SUCCESS : 0;
    }
    
    private static int get(CommandContext<ServerCommandSource> context) {
        Int2ObjectMap<QuestInstance> activeQuests = getActiveQuests(context);
        try {
            int id = context.getArgument("id", Integer.class);
            QuestInstance quest = activeQuests.get(id);
            if (quest == null) {
                context.getSource().sendError(Text.translatable("commands.quest.error", id));
                return 0;
            }
            getQuest(context, quest);
            return SINGLE_SUCCESS;
        } catch (Exception e) {
            for (int id : activeQuests.keySet()) {
                QuestInstance quest = activeQuests.get(id);
                getQuest(context, quest);
            }
            return activeQuests.size();
        }
    }
    private static void getQuest(CommandContext<ServerCommandSource> context, QuestInstance quest) {
        MutableText text = getQuestName(quest);
        context.getSource().sendFeedback(() -> text, false);
    }
    
    private static int check(CommandContext<ServerCommandSource> context) {
        int completable = 0;
        try {
            int id = context.getArgument("id", Integer.class);
            completable = check(context, id) ? 1 : 0;
        } catch (Exception e) {
            for (int id : getActiveQuests(context).keySet()) completable += check(context, id) ? 1 : 0;
        }
        return completable;
    }
    private static boolean check(CommandContext<ServerCommandSource> context, int id) {
        QuestInstance quest = getActiveQuests(context).get(id);
        if (quest == null) {
            context.getSource().sendError(Text.translatable("commands.quest.error", id));
            return false;
        }
        ServerPlayerEntity player;
        try {
            player = context.getArgument("player", ServerPlayerEntity.class);
        } catch (IllegalArgumentException e) {
            player = context.getSource().getPlayer();
        }
        assert player != null;
        boolean canComplete = checkQuest(player, id);
        MutableText text = Text.translatable("commands.quest.check." + canComplete, player.getName(), getQuestName(quest));
        context.getSource().sendFeedback(() -> text, false);
        return canComplete;
    }
    
    private static int claim(CommandContext<ServerCommandSource> context) {
        int completed = 0;
        try {
            int id = context.getArgument("id", Integer.class);
            completed = claim(context, id) ? 1 : 0;
        } catch (Exception e) {
            for (int id : getActiveQuests(context).keySet()) completed += claim(context, id) ? 1 : 0;
        }
        return completed;
    }
    private static boolean claim(CommandContext<ServerCommandSource> context, int id) {
        QuestInstance quest = getActiveQuests(context).get(id);
        if (quest == null) {
            context.getSource().sendError(Text.translatable("commands.quest.error", id));
            return false;
        }
        ServerPlayerEntity player;
        try {
            player = context.getArgument("player", ServerPlayerEntity.class);
        } catch (IllegalArgumentException e) {
            player = context.getSource().getPlayer();
        }
        boolean check = true;
        try {
            check = context.getArgument("check", Boolean.class);
        } catch (IllegalArgumentException ignored) {}
        assert player != null;
        return completeQuest(player, id, !check);
    }
    
    private static int remove(CommandContext<ServerCommandSource> context) {
        int id = context.getArgument("id", Integer.class);
        QuestInstance quest = getActiveQuests(context).remove(id);
        if (quest == null) {
            context.getSource().sendError(Text.translatable("commands.quest.error", id));
            return 0;
        }
        return SINGLE_SUCCESS;
    }
    private static int clear(CommandContext<ServerCommandSource> context) {
        
        getActiveQuests(context).clear();
        return SINGLE_SUCCESS;
    }
    
    private static int menu(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.translatable("commands.quest.error.no_player"));
            return 0;
        }
        updateQuests(player);
        return SINGLE_SUCCESS;
    }
    
    private static CompletableFuture<Suggestions> getIdSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        getActiveQuests(context).keySet()
                .intStream()
                .filter(i -> Integer.toString(i).endsWith(builder.getRemaining()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
    
    private static Int2ObjectMap<QuestInstance> getActiveQuests(CommandContext<ServerCommandSource> context) {
        return QuestData.getActiveQuests(context.getSource().getServer());
    }
}
