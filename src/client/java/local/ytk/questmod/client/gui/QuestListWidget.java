package local.ytk.questmod.client.gui;

import local.ytk.questmod.quests.QuestStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.List;

public class QuestListWidget extends ElementListWidget<QuestListEntry> {
    final List<QuestStatus> quests;
//    WorldListWidget
    
    public QuestListWidget(MinecraftClient minecraftClient, int width, int height, int y, int itemHeight, List<QuestStatus> quests) {
        super(minecraftClient, width, height, y, itemHeight);
        this.quests = quests;
        quests.forEach(quest -> {
            QuestListEntry entry = new QuestListEntry(quest, this);
            addEntry(entry);
        });
    }
    
    @Override
    public boolean removeEntry(QuestListEntry entry) {
        return super.removeEntry(entry);
    }
}
