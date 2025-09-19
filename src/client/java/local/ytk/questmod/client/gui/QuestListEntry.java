package local.ytk.questmod.client.gui;

import local.ytk.questmod.Util;
import local.ytk.questmod.client.QuestModClient;
import local.ytk.questmod.quests.QuestStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Supplier;

public class QuestListEntry extends ElementListWidget.Entry<QuestListEntry> {
    final MinecraftClient client = MinecraftClient.getInstance();
    final TextRenderer textRenderer = client.textRenderer;
    final ButtonWidget claimButton;
    final List<Selectable> buttons;
    final List<Element> elements;
    final Slot rewardSlot;
    
    final QuestStatus quest;
    final int questId;
    final ScoreboardCriterion score;
    final int total;
    final int count;
    final float progress;
    final ItemStack rewardItem;
    final int rewardXp;
    
    int currentX;
    int currentY;
    
    public QuestListEntry(QuestStatus quest) {
        claimButton = new QuestClaimButtonWidget(140, 4, 50, 20, quest.progress() >= 1f);
        
        rewardSlot = new Slot(null, 0, 0, 0) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
            @Override
            public boolean canTakeItems(PlayerEntity playerEntity) {
                return false;
            }
            @Override
            public ItemStack getStack() {
                return rewardItem;
            }
        };
        buttons = List.of(claimButton);
        elements = List.of(claimButton);
        
        this.quest = quest;
        questId = quest.quest().id();
        score = quest.quest().target().score();
        total = quest.total();
        count = quest.count();
        progress = quest.progress();
        rewardItem = quest.quest().reward().item().copy();
        rewardXp = quest.quest().reward().xp();
    }
    
    @Override
    public List<? extends Selectable> selectableChildren() {
        return buttons;
    }
    
    @Override
    public List<? extends Element> children() {
        return elements;
    }
    
    
    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress) {
        currentX = x;
        currentY = y;
        context.drawBorder(x, y, entryWidth, entryHeight, -1);
        Text name = getName(count);
        context.drawWrappedTextWithShadow(textRenderer, name, x + 5, y + 4, 130, -1);
        drawReward(context, y, x, entryWidth, entryHeight, mouseX, mouseY);
        claimButton.render(context, mouseX, mouseY, tickProgress);
    }
    
    private void drawReward(DrawContext context, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY) {
        int x1 = x + entryWidth - 20;
        int y1 = y + entryHeight / 2 - 9;
        context.drawItem(rewardItem, x1, y1);
        context.drawStackOverlay(textRenderer, rewardItem, x1, y1);
        // Tooltip
        if (mouseX >= x1 - 1 && mouseX < x1 + 16 + 1 && mouseY >= y1 - 1 && mouseY < y1 + 16 + 1) {
            Item.TooltipContext tooltipContext = Item.TooltipContext.create(client.world);
            TooltipType.Default tooltipType = client.options.advancedItemTooltips ? TooltipType.Default.ADVANCED : TooltipType.Default.BASIC;
            List<Text> tooltip = rewardItem.getTooltip(tooltipContext, client.player, tooltipType);
            context.drawTooltip(textRenderer, tooltip, rewardItem.getTooltipData(), mouseX, mouseY, rewardItem.get(DataComponentTypes.TOOLTIP_STYLE));
        }
    }
    
    public Text getName(int count) {
        return Util.getScoreName(score, count);
    }
    
    public void claim(ButtonWidget buttonWidget) {
        QuestModClient.claimQuest(questId);
    }
    
    private class QuestClaimButtonWidget extends ButtonWidget {
        final int xOffset;
        final int yOffset;
        public QuestClaimButtonWidget(int x, int y, int w, int h, boolean complete) {
            super(x, y, w, h, Text.translatable("gui.quests.claim"), QuestListEntry.this::claim, Supplier::get);
            xOffset = x;
            yOffset = y;
            active = complete;
        }
        
        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            super.renderWidget(context, mouseX, mouseY, deltaTicks);
            setPosition(xOffset + currentX, yOffset + currentY);
        }
    }
    
    
}
