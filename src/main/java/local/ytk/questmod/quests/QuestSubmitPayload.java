package local.ytk.questmod.quests;

import local.ytk.questmod.QuestMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record QuestSubmitPayload(int id) implements CustomPayload {
    public static final Identifier ID = QuestMod.id("quest_submit");
    public static final CustomPayload.Id<QuestSubmitPayload> PAYLOAD_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, QuestSubmitPayload> PACKET_CODEC
            = PacketCodecs.INTEGER.xmap(QuestSubmitPayload::new, QuestSubmitPayload::id).cast();
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
