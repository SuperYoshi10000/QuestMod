package local.ytk.questmod.client.gui;

import local.ytk.questmod.quests.QuestStatus;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.List;

public class QuestScreen extends Screen {
    final List<QuestStatus> quests;
    QuestListWidget questList;
    ButtonWidget claimAllButton;
    
    public QuestScreen(List<QuestStatus> quests) {
        super(Text.translatable("gui.quests.title"));
        this.quests = quests;
        
    }
    
    @Override
    protected void init() {
        super.init();
        this.questList = new QuestListWidget(client, width, height, 40, 32, quests);
        this.claimAllButton = ButtonWidget.builder(Text.translatable("gui.quests.claim_all"), this::claimAll)
                .position(width / 2 - 50, 16)
                .size(100, 20)
                .build();
        addDrawableChild(questList);
        addDrawableChild(claimAllButton);
    }
    
    private void claimAll(ButtonWidget buttonWidget) {
        questList.children().forEach(e -> e.claim(buttonWidget));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.quests.title"), width / 2, 5, Colors.WHITE);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
