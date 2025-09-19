package local.ytk.questmod;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import local.ytk.questmod.quests.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceType;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static local.ytk.questmod.Util.*;

public class QuestMod implements ModInitializer {
    public static final String MOD_ID = "questmod";
    
    public static final GameRules.Key<GameRules.BooleanRule> ENABLE_QUESTS
            = GameRuleRegistry.register("enableQuests", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> ENABLE_DEATH_QUESTS
            = GameRuleRegistry.register("enableDeathQuests", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> ENABLE_PVP_QUESTS
            = GameRuleRegistry.register("enablePvpQuests", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
    public static final GameRules.Key<GameRules.IntRule> NEW_QUEST_DELAY
            = GameRuleRegistry.register("newQuestDelay", GameRules.Category.MISC, GameRuleFactory.createIntRule(1200));
    public static final TagKey<Item> QUEST_ITEM_TARGET = TagKey.of(RegistryKeys.ITEM, id("quest_item_target"));
    
    public static final QuestLoader QUEST_LOADER = new QuestLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestMod.class);
    
    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(QUEST_LOADER);
        
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            QuestCommand.register(dispatcher);
        });
        ServerTickEvents.END_SERVER_TICK.register(QuestMod::createQuestIfNeeded);
        
        PayloadTypeRegistry.playS2C().register(QuestPayload.PAYLOAD_ID, QuestPayload.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(QuestPayload.PAYLOAD_ID, QuestPayload.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(QuestSubmitPayload.PAYLOAD_ID, QuestSubmitPayload.PACKET_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(QuestPayload.PAYLOAD_ID, QuestMod::receiveQuestPayload);
        ServerPlayNetworking.registerGlobalReceiver(QuestSubmitPayload.PAYLOAD_ID, QuestMod::receiveQuestSubmitPayload);
    }
    
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
    
    private static void createQuestIfNeeded(MinecraftServer server) {
        long time = server.getOverworld().getTime();
        QuestData state = QuestData.getServerState(server);
        if (time > state.lastQuestTick + server.getGameRules().getInt(NEW_QUEST_DELAY)) {
            createQuest(server);
            state.lastQuestTick = time;
        }
    }
    
    private static void receiveQuestPayload(QuestPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        updateQuests(player);
    }
    public static void receiveQuestSubmitPayload(QuestSubmitPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        int id = payload.id();
        completeQuest(player, id, false);
    }
    
    public static boolean createQuest(MinecraftServer server) {
        QuestInstance quest = QUEST_LOADER.questMaker.createNewQuest(server);
        if (quest == null) return false;
        createQuestScore(server, quest);
        QuestData.getActiveQuests(server).put(quest.id(), quest);
        return true;
    }
    
    public static void updateQuests(ServerPlayerEntity player) {
        Int2ObjectMap<QuestInstance> quests = QuestData.getActiveQuests(player.getServer());
        QuestData.PlayerQuestData data = QuestData.getPlayerState(player);
        Int2IntMap startingValues = data.questStartingValues();
        List<QuestStatus> statuses = new ArrayList<>(quests.size());
        for (int id : quests.keySet()) {
            QuestInstance quest = quests.get(id);
            int start = startingValues.get(id);
            QuestTarget type = quest.target();
            int current = getQuestScore(player, id);
            int total = current - start;
            int count = type.count();
            float progress = Math.clamp((float) total / (float) count, 0, 1);
            QuestStatus status = new QuestStatus(quest, total, count, progress);
            statuses.add(status);
        }
        QuestPayload payload = new QuestPayload(statuses);
        ServerPlayNetworking.send(player, payload);
    }
    public static boolean checkQuest(ServerPlayerEntity player, int id) {
        MinecraftServer server = player.getServer();
        if (server == null) return false; // Client side (should not happen)
        Int2ObjectMap<QuestInstance> activeQuests = QuestData.getActiveQuests(server);
        return getQuestIfComplete(player, id, false, activeQuests) != null;
    }
    public static boolean completeQuest(ServerPlayerEntity player, int id, boolean noCheck) {
        MinecraftServer server = player.getServer();
        if (server == null) return false; // Client side (should not happen)
        Int2ObjectMap<QuestInstance> activeQuests = QuestData.getActiveQuests(server);
        QuestInstance quest = getQuestIfComplete(player, id, noCheck, activeQuests);
        if (quest == null) return false;
        QuestReward reward = quest.reward();
        player.giveOrDropStack(reward.item());
        player.addExperience(reward.xp());
        activeQuests.remove(id); // Prevent others from completing the same quest
        
        // Announce that the player has completed the quest
        Text playerText = player.getDisplayName();
        Text questText = getQuestName(quest);
        server.sendMessage(Text.translatable("chat.type.quest", playerText, questText));
        return true;
    }
    
    private static @Nullable QuestInstance getQuestIfComplete(ServerPlayerEntity player, int id, boolean noCheck, Int2ObjectMap<QuestInstance> activeQuests) {
        QuestData.PlayerQuestData data = QuestData.getPlayerState(player);
        int score = getQuestScore(player, id);
        int start = data.questStartingValues().get(id);
        QuestInstance quest = activeQuests.get(id);
        if (quest == null) return null;
        int count = quest.target().count();
        if (score - start < count && !noCheck) return null;
        return quest;
    }
    
    
    public static void createQuestScore(MinecraftServer server, QuestInstance quest) {
        int id = quest.id();
        String name = "QUESTS.id_" + Integer.toHexString(id);
        Text displayName = getQuestName(quest);
        ServerScoreboard scoreboard = server.getScoreboard();
        scoreboard.addObjective(name, quest.target().score(), displayName, ScoreboardCriterion.RenderType.INTEGER, false, BlankNumberFormat.INSTANCE);
        QuestData.getPlayerData(server).forEach((playerId, data) -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) return;
            data.questStartingValues().put(id, getQuestScore(player, id));
        });
    }
    public static int getQuestScore(ServerPlayerEntity player, int id) {
        String name = "QUESTS.id_" + Integer.toHexString(id);
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective nullableObjective = scoreboard.getNullableObjective(name);
        if (nullableObjective == null) {
            MinecraftServer server = player.getServer();
            if (server == null) {
                LOGGER.error("Could not get server from player {}", player.getName().getString());
                return 0;
            }
            QuestInstance quest = QuestData.getActiveQuests(server).get(id);
            createQuestScore(server, quest);
            return 0;
        }
        ScoreAccess score = scoreboard.getOrCreateScore(player, nullableObjective);
        return score != null ? score.getScore() : 0;
    }
}
