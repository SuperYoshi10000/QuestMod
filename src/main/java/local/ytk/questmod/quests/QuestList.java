package local.ytk.questmod.quests;

import local.ytk.questmod.QuestMod;
import local.ytk.questmod.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class QuestList {
    public static final int DEFAULT_WEIGHT = 1000;
    public final Identifier id;
    int weight;
    List<Target<?>> targets = new ArrayList<>();
    List<Reward> rewards = new ArrayList<>();
    BlockModifiers blockModifiers;
    ItemModifiers itemModifiers;
    
    
    public QuestList(Identifier id) {
        this(id, DEFAULT_WEIGHT);
    }
    public QuestList(Identifier id, int weight) {
        this.id = id;
        this.weight = weight;
    }
    
    public void addTarget(Target<?> target) {
        targets.add(target);
    }
    public void addReward(Reward reward) {
        rewards.add(reward);
    }
    
    public int weight() {
        return weight;
    }
    
    public static QuestList select(Collection<QuestList> lists, Random random) {
        return Util.getWeightedRandom(List.copyOf(lists), QuestList::weight, random);
    }
    
    public interface Target<T> {
        QuestList list();
        StatType<T> statType();
        T statValue();
        int base();
        int value();
        default boolean isDeathQuest() {
            return false;
        }
        default boolean isPvpQuest() {
            return false;
        }
        default String statName() {
            return Stat.getName(statType(), statValue());
        }
        default Optional<ScoreboardCriterion> stat() {
            return Stat.getOrCreateStatCriterion(statName());
        }
    }
    public record SimpleTarget(QuestList list, ScoreboardCriterion criterion, int base, int value, boolean isDeathQuest, boolean isPvpQuest) implements Target<ScoreboardCriterion> {
        @Override
        public StatType<ScoreboardCriterion> statType() {
            return null;
        }
        @Override
        public ScoreboardCriterion statValue() {
            return criterion;
        }
        
        @Override
        public String statName() {
            return criterion.getName();
        }
    }
    public record BaseTarget<T>(QuestList list, StatType<T> statType, T statValue, int base, int value, boolean isDeathQuest, boolean isPvpQuest) implements Target<T> { }
    public record BlockTarget(QuestList list, StatType<Block> statType, Block statValue) implements Target<Block> {
        @Override
        public int base() {
            return list.blockModifiers.getBlockToolLevel(statValue);
        }
        @Override
        public int value() {
            BlockModifiers modifiers = list.blockModifiers;
            return modifiers.value + (int) Math.ceil(statValue.getHardness() * modifiers.hardnessMultiplier);
        }
        
    }
    public record ItemTarget(QuestList list, StatType<Item> statType, Item statValue) implements Target<Item> {
        static boolean isUsable(Item item) {
            ItemStack stack = item.getDefaultStack();
            return stack.isDamageable()
                    || stack.getUseAction() != UseAction.NONE
                    || stack.contains(DataComponentTypes.CONSUMABLE)
                    || stack.contains(DataComponentTypes.FOOD)
                    || stack.contains(DataComponentTypes.POTION_CONTENTS)
                    || stack.contains(DataComponentTypes.TOOL)
                    || stack.contains(DataComponentTypes.WEAPON)
                    || stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)
                    || stack.contains(DataComponentTypes.DEATH_PROTECTION)
                    || stack.contains(DataComponentTypes.USE_COOLDOWN)
                    || stack.isIn(QuestMod.QUEST_ITEM_TARGET);
        }
        
        @Override
        public int base() {
            ItemModifiers modifiers = list.itemModifiers;
            return switch (statValue.getDefaultStack().getRarity()) {
                case COMMON -> modifiers.commonBase;
                case UNCOMMON -> modifiers.uncommonBase;
                case RARE -> modifiers.rareBase;
                case EPIC -> modifiers.epicBase;
            };
        }
        @Override
        public int value() {
            ItemModifiers modifiers = list.itemModifiers;
            return switch (statValue.getDefaultStack().getRarity()) {
                case COMMON -> modifiers.common;
                case UNCOMMON -> modifiers.uncommon;
                case RARE -> modifiers.rare;
                case EPIC -> modifiers.epic;
            };
        }
    }
    public record EntityTarget(QuestList list, StatType<EntityType<?>> statType, EntityType<?> statValue, int base, int value) implements Target<net.minecraft.entity.EntityType<?>> {
        @SuppressWarnings("unchecked")
        public <T extends Entity> EntityType<T> castValue() {
            return (EntityType<T>) statValue;
        }
    }
    
    
    public record Reward(QuestList list, Item item, int base, int value) {}
    
    public record BlockModifiers(int noTool, int wood, int stone, int iron, int diamond, int value, float hardnessMultiplier) {
        int getBlockToolLevel(Block block) {
            BlockState state = block.getDefaultState();
            if (!state.isToolRequired()) return noTool;
            if (state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)) return diamond;
            if (state.isIn(BlockTags.NEEDS_IRON_TOOL)) return iron;
            if (state.isIn(BlockTags.NEEDS_STONE_TOOL)) return stone;
            return wood;
        }
    }
    public record ItemModifiers(int commonBase, int common, int uncommonBase, int uncommon, int rareBase, int rare, int epicBase, int epic) {}
    
    @FunctionalInterface
    public interface TargetFactory<T> {
        Target<T> create(QuestList list, StatType<T> statType, T statValue);
        static <T> Identifier getId(StatType<T> statType, T statValue) {
            return statType.getRegistry().getId(statValue);
        }
    }
}
