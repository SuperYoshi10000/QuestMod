package local.ytk.questmod.quests;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import local.ytk.questmod.QuestMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.*;

public class QuestData extends PersistentState {
    public static final Codec<QuestData> CODEC = NbtCompound.CODEC.xmap(QuestData::fromNbt, QuestData::toNbt);
    private static final PersistentStateType<QuestData> TYPE = new PersistentStateType<>(
            QuestMod.MOD_ID, QuestData::new, CODEC, null
    );
    
    public final List<QuestInstance> activeQuests = new ArrayList<>();
    public final HashMap<UUID, PlayerQuestData> players = new HashMap<>();
    
    public PlayerQuestData getPlayerData(UUID uuid) {
        return players.computeIfAbsent(uuid, PlayerQuestData::new);
    }
    
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList questListTag = activeQuests.stream()
                .map(quest -> QuestInstance.CODEC.encodeStart(NbtOps.INSTANCE, quest))
                .map(DataResult::result)
                .flatMap(Optional::stream)
                .collect(NbtList::new, NbtList::add, NbtList::addAll);
        nbt.put("quests", questListTag);
        
        NbtCompound playerListTag = new NbtCompound();
        for (UUID uuid : players.keySet()) {
            NbtCompound playerTag = new NbtCompound();
            playerTag.put("quests", getPlayerData(uuid).toNbt());
            playerListTag.put(uuid == null ? "PLAYER" : uuid.toString(), playerTag);
        }
        nbt.put("players", playerListTag);
        return nbt;
    }
    
    public NbtCompound toNbt() {
        return writeNbt(new NbtCompound());
    }
    
    public static QuestData fromNbt(NbtCompound nbt) {
        QuestData state = new QuestData();
        NbtList questListTag = nbt.getListOrEmpty("quests");
        questListTag.stream()
                .map(NbtElement::asCompound)
                .flatMap(Optional::stream)
                .map(c -> QuestInstance.CODEC.decode(NbtOps.INSTANCE, c))
                .map(DataResult::result)
                .flatMap(Optional::stream)
                .map(Pair::getFirst)
                .forEach(state.activeQuests::add);
        
        NbtCompound playerListTag = nbt.getCompoundOrEmpty("players");
        for (String key : playerListTag.getKeys()) {
            UUID uuid = key.equals("PLAYER") ? null : UUID.fromString(key);
            NbtCompound playerTag = nbt.getCompoundOrEmpty(key);
            PlayerQuestData playerState = PlayerQuestData.fromNbt(playerTag);
            state.players.put(uuid, playerState);
        }
        return state;
    }
    public static QuestData getServerState(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        assert world != null;
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
        QuestData state = getServerState(server);
        PlayerQuestData playerState = state.players.computeIfAbsent(player.getUuid(),
                uuid -> state.players.computeIfAbsent(null, PlayerQuestData::new));
        if (server.isSingleplayer()) state.players.put(null, playerState); // null represents the main player in a singleplayer world
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
