package local.ytk.questmod.quests;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.*;

public class QuestData extends PersistentState {
    public static final String QUESTS_DATA_ID = "quests";
    public static final Codec<QuestData> CODEC = NbtCompound.CODEC.xmap(QuestData::fromNbt, QuestData::toNbt);
    private static final PersistentStateType<QuestData> TYPE = new PersistentStateType<>(
            QUESTS_DATA_ID, QuestData::new, CODEC, null
    );
    
    public long lastQuestTick = 0;
    public int nextQuestId = 1;
    public final Int2ObjectLinkedOpenHashMap<QuestInstance> activeQuests = new Int2ObjectLinkedOpenHashMap<>();
    public final HashMap<UUID, PlayerQuestData> players = new HashMap<>();
    
    public static int getNextQuestId(MinecraftServer server) {
        return getServerState(server).nextQuestId++;
    }
    public static Int2ObjectMap<QuestInstance> getActiveQuests(MinecraftServer server) {
        return getServerState(server).activeQuests;
    }
    public static HashMap<UUID, PlayerQuestData> getPlayerData(MinecraftServer server) {
        return getServerState(server).players;
    }
    
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound questListTag = activeQuests
                .int2ObjectEntrySet()
                .stream()
                .flatMap(questEntry -> QuestInstance.CODEC
                        .encodeStart(NbtOps.INSTANCE, questEntry.getValue())
                        .map(e -> Pair.of(Integer.toHexString(questEntry.getIntKey()), e))
                        .result()
                        .stream()
                )
                .collect(NbtCompound::new,
                        (m, p) -> m.put(p.getFirst(), p.getSecond()),
                        (a, b) -> b.forEach(a::put)
                );
        nbt.put("quests", questListTag);
        
        NbtCompound playerListTag = new NbtCompound();
        for (UUID uuid : players.keySet()) {
            NbtCompound playerTag = new NbtCompound();
            playerTag.put("quests", players.computeIfAbsent(uuid, PlayerQuestData::new).toNbt());
            playerListTag.put(uuid == null ? "PLAYER" : uuid.toString(), playerTag);
        }
        nbt.put("players", playerListTag);
        
        nbt.putInt("nextQuestId", nextQuestId);
        nbt.putLong("lastQuestTick", lastQuestTick);
        return nbt;
    }
    
    public NbtCompound toNbt() {
        return writeNbt(new NbtCompound());
    }
    
    public static QuestData fromNbt(NbtCompound nbt) {
        QuestData state = new QuestData();
        NbtCompound questListTag = nbt.getCompoundOrEmpty("quests");
        for (String key : questListTag.getKeys()) {
            NbtElement element = questListTag.get(key);
            if (!(element instanceof NbtCompound compound)) continue;
            Optional<QuestInstance> quest = QuestInstance.CODEC.decode(NbtOps.INSTANCE, compound)
                    .map(Pair::getFirst)
                    .result();
            if (quest.isEmpty()) continue;
            state.activeQuests.put(Integer.parseUnsignedInt(key, 16), quest.get());
        }
        NbtCompound playerListTag = nbt.getCompoundOrEmpty("players");
        for (String key : playerListTag.getKeys()) {
            UUID uuid = key.equals("PLAYER") ? null : UUID.fromString(key);
            NbtCompound playerTag = nbt.getCompoundOrEmpty(key);
            PlayerQuestData playerState = PlayerQuestData.fromNbt(playerTag);
            state.players.put(uuid, playerState);
        }
        
        nbt.getInt("nextQuestId").ifPresent(i -> state.nextQuestId = i);
        nbt.getLong("lastQuestTick").ifPresent(l -> state.lastQuestTick = l);
        return state;
    }
    public static QuestData getServerState(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        QuestData state = world.getPersistentStateManager().getOrCreate(TYPE);
        state.markDirty();
        return state;
    }
    public static PlayerQuestData getPlayerState(PlayerEntity player) {
        assert player != null;
        MinecraftServer server = player.getServer();
        if (server == null) {
            // Client side
            return new PlayerQuestData(player.getUuid());
        }
        HashMap<UUID, PlayerQuestData> playerData = getPlayerData(server);
        PlayerQuestData playerState = playerData.computeIfAbsent(player.getUuid(),
                uuid -> playerData.computeIfAbsent(null, PlayerQuestData::new));
        if (server.isSingleplayer()) playerData.put(null, playerState); // null represents the main player in a singleplayer world
        return playerState;
    }
    
    public record PlayerQuestData(Int2IntMap questStartingValues) {
        public PlayerQuestData(UUID uuid) {
            this(new Int2IntOpenHashMap());
        }
        public NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            questStartingValues.forEach((k, v) -> tag.putInt(Integer.toHexString(k), v));
            return tag;
        }
        public static PlayerQuestData fromNbt(NbtCompound nbt) {
            Int2IntMap questStartingValues = new Int2IntOpenHashMap();
            nbt.forEach((k, v) -> questStartingValues.put(Integer.parseUnsignedInt(k, 16), (int) v.asInt().orElse(0)));
            return new PlayerQuestData(questStartingValues);
        }
    }
}
