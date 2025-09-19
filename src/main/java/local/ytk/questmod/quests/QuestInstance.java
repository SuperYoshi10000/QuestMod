package local.ytk.questmod.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record QuestInstance(int id, QuestTarget target, QuestReward reward) {
    public static final MapCodec<QuestInstance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(QuestInstance::id),
            QuestTarget.MAP_CODEC.forGetter(QuestInstance::target),
            QuestReward.MAP_CODEC.forGetter(QuestInstance::reward)
    ).apply(instance, QuestInstance::new));
    public static final Codec<QuestInstance> CODEC = MAP_CODEC.codec();
    public static final PacketCodec<RegistryByteBuf, QuestInstance> PACKET_CODEC = PacketCodec.ofStatic(
            QuestInstance::write, QuestInstance::read
    );
    
    public static QuestInstance read(RegistryByteBuf buf) {
        return new QuestInstance(buf.readInt(), QuestTarget.read(buf), QuestReward.read(buf));
    }
    public static void write(RegistryByteBuf buf, QuestInstance quest) {
        buf.writeInt(quest.id);
        QuestTarget.write(buf, quest.target);
        QuestReward.write(buf, quest.reward);
    }
}
