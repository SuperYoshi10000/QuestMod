package local.ytk.questmod.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record QuestReward(ItemStack item, int xp) {
    public static final MapCodec<QuestReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC.fieldOf("reward").forGetter(QuestReward::item),
            Codec.INT.fieldOf("xp").forGetter(QuestReward::xp)
    ).apply(instance, QuestReward::new));
    public static final Codec<QuestReward> CODEC = MAP_CODEC.codec();
    public static final PacketCodec<RegistryByteBuf, QuestReward> PACKET_CODEC = PacketCodec.ofStatic(
            QuestReward::write, QuestReward::read
    );
    
    public static QuestReward read(RegistryByteBuf buf) {
        ItemStack item = ItemStack.PACKET_CODEC.decode(buf);
        int xp = buf.readInt();
        return new QuestReward(item, xp);
    }
    public static void write(RegistryByteBuf buf, QuestReward reward) {
        ItemStack.PACKET_CODEC.encode(buf, reward.item);
        buf.writeInt(reward.xp);
    }
}
