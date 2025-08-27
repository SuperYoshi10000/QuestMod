package local.ytk.questmod.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class QuestScreen extends Screen {
    public QuestScreen() {
        super(Text.translatable("gui.quests.title"));
    }
}
