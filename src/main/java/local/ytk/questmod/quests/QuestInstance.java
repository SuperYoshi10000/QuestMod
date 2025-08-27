package local.ytk.questmod.quests;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record QuestInstance(QuestType type, QuestReward reward) {
    public static final Codec<QuestInstance> CODEC = Codec.pair(QuestType.CODEC, QuestReward.CODEC).xmap(QuestInstance::new, QuestInstance::toPair);
    public static final PacketCodec<RegistryByteBuf, QuestInstance> PACKET_CODEC = PacketCodec.ofStatic(
            QuestInstance::write, QuestInstance::read
    );
    public QuestInstance(Pair<QuestType, QuestReward> p) {
        this(p.getFirst(), p.getSecond());
    }
    Pair<QuestType, QuestReward> toPair() {
        return Pair.of(type, reward);
    }
    
    public static QuestInstance read(RegistryByteBuf buf) {
        return new QuestInstance(QuestType.read(buf), QuestReward.read(buf));
    }
    public static void write(RegistryByteBuf buf, QuestInstance quest) {
        QuestType.write(buf, quest.type);
        QuestReward.write(buf, quest.reward);
    }
}
