package local.ytk.questmod.quests;

import local.ytk.questmod.QuestMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public record QuestPayload(List<QuestInstance> quests) implements CustomPayload {
    public static final Identifier ID = QuestMod.id("quest");
    public static final CustomPayload.Id<QuestPayload> PAYLOAD_ID = new Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, QuestPayload> PACKET_CODEC = PacketCodec.ofStatic(
        QuestPayload::write, QuestPayload::read
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestPayload.class);
    
    public static QuestPayload read(RegistryByteBuf buf) {
        int length = buf.readInt();
        List<QuestInstance> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            QuestInstance quest = QuestInstance.read(buf);
            list.add(quest);
        }
        return new QuestPayload(list);
    }
    public static void write(RegistryByteBuf buf, QuestPayload payload) {
        buf.writeInt(payload.quests.size());
        payload.quests.forEach(quest -> QuestInstance.write(buf, quest));
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
