package local.ytk.questmod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.List;

public class QuestListEntry extends ElementListWidget.Entry<QuestListEntry> {
    final List<ClickableWidget> buttons;
    
    @Override
    public List<? extends Selectable> selectableChildren() {
        return List.of();
    }
    
    @Override
    public List<? extends Element> children() {
        return List.of();
    }
    
    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress) {
    
    }
}
