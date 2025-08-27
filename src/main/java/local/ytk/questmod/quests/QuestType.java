package local.ytk.questmod.quests;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.stat.StatType;

public record QuestType<T>(StatType<T> stat, int count) {
    public static final Codec<QuestType<?>> CODEC = null;
    public static final PacketCodec<RegistryByteBuf, QuestType<?>> PACKET_CODEC = PacketCodec.ofStatic(
            QuestType::write, QuestType::read
    );
    static QuestType<?> read(RegistryByteBuf buf) {
    
    }
    static void write(RegistryByteBuf buf, QuestType<?> type) {
    
    }
}
