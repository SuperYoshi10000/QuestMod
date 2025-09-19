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

public record QuestPayload(List<QuestStatus> quests) implements CustomPayload {
    public static final Identifier ID = QuestMod.id("quest");
    public static final CustomPayload.Id<QuestPayload> PAYLOAD_ID = new Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, QuestPayload> PACKET_CODEC = PacketCodec.ofStatic(
        QuestPayload::write, QuestPayload::read
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestPayload.class);
    
    public static final QuestPayload EMPTY = new QuestPayload(null);
    
    public static QuestPayload read(RegistryByteBuf buf) {
        int length = buf.readInt();
        if (length < 0) {
            if (length != -1) LOGGER.warn("Quest length is negative: {}", length);
            return new QuestPayload(null);
        }
        List<QuestStatus> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            QuestStatus quest = QuestStatus.read(buf);
            list.add(quest);
        }
        return new QuestPayload(list);
    }
    public static void write(RegistryByteBuf buf, QuestPayload payload) {
        if (payload.quests == null) {
            buf.writeInt(-1);
            return;
        }
        buf.writeInt(payload.quests.size());
        payload.quests.forEach(quest -> QuestStatus.write(buf, quest));
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
