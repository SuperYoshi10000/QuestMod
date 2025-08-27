package local.ytk.questmod.quests;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record QuestReward(ItemStack item, int xp) {
    public static final Codec<QuestReward> CODEC = Codec.pair(ItemStack.CODEC, Codec.INT).xmap(QuestReward::new, QuestReward::toPair);
    public static final PacketCodec<RegistryByteBuf, QuestReward> PACKET_CODEC = PacketCodec.ofStatic(
            QuestReward::write, QuestReward::read
    );
    
    public QuestReward(Pair<ItemStack, Integer> p) {
        this(p.getFirst(), p.getSecond());
    }
    Pair<ItemStack, Integer> toPair() {
        return Pair.of(item, xp);
    }
    public static QuestReward read(RegistryByteBuf buf) {
        ItemStack item = ItemStack.PACKET_CODEC.decode(buf);
        int xp = buf.readInt();
        return new QuestReward(item, xp);
    }
    public static void write(RegistryByteBuf buf, QuestReward reward) {
        ItemStack.PACKET_CODEC.encode(buf, reward.item);
        
    }
}
