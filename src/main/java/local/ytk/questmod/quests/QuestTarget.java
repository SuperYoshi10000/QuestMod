package local.ytk.questmod.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.scoreboard.ScoreboardCriterion;

import java.util.Optional;

public record QuestTarget(ScoreboardCriterion score, int count) {
    public static final MapCodec<QuestTarget> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ScoreboardCriterion.CODEC.fieldOf("score").forGetter(a -> a.score),
            Codec.INT.fieldOf("count").forGetter(QuestTarget::count)
    ).apply(instance, QuestTarget::new));
//    public static final PacketCodec<RegistryByteBuf, ScoreboardCriterion> SCORE_PACKET_CODEC = PacketCodecs.codec(ScoreboardCriterion.CODEC).cast();
    
    public static final Codec<QuestTarget> CODEC = MAP_CODEC.codec();
    public static final PacketCodec<RegistryByteBuf, QuestTarget> PACKET_CODEC = PacketCodec.ofStatic(
            QuestTarget::write, QuestTarget::read
    );
    
    
    static QuestTarget read(RegistryByteBuf buf) {
        String scoreName = buf.readString();
        Optional<ScoreboardCriterion> score = ScoreboardCriterion.getOrCreateStatCriterion(scoreName);
        return new QuestTarget(score.orElseThrow(IllegalArgumentException::new), buf.readInt());
    }
    static void write(RegistryByteBuf buf, QuestTarget type) {
//        SCORE_PACKET_CODEC.encode(buf, type.score);
        String scoreName = type.score.getName();
        buf.writeString(scoreName);
        buf.writeInt(type.count);
    }
}
