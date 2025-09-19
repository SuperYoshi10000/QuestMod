package local.ytk.questmod.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record QuestStatus(QuestInstance quest, int total, int count, float progress) {
    public static final MapCodec<QuestStatus> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            QuestInstance.MAP_CODEC.forGetter(QuestStatus::quest),
            Codec.INT.fieldOf("total").forGetter(QuestStatus::total),
            Codec.INT.fieldOf("count").forGetter(QuestStatus::count),
            Codec.FLOAT.fieldOf("progress").forGetter(QuestStatus::progress)
    ).apply(instance, QuestStatus::new));
    public static final Codec<QuestStatus> CODEC = MAP_CODEC.codec();
    public static final PacketCodec<RegistryByteBuf, QuestStatus> PACKET_CODEC = PacketCodec.ofStatic(
            QuestStatus::write, QuestStatus::read
    );
    
    public static QuestStatus read(RegistryByteBuf buf) {
        return new QuestStatus(QuestInstance.read(buf), buf.readInt(), buf.readInt(), buf.readFloat());
    }
    public static void write(RegistryByteBuf buf, QuestStatus quest) {
        QuestInstance.write(buf, quest.quest);
        buf.writeInt(quest.total);
        buf.writeInt(quest.count);
        buf.writeFloat(quest.progress);
    }
    
}
