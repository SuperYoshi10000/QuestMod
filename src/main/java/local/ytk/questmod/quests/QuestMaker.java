package local.ytk.questmod.quests;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import local.ytk.questmod.QuestMod;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static local.ytk.questmod.QuestMod.QUEST_LOADER;
import static local.ytk.questmod.Util.*;
import static net.minecraft.util.Util.*;

public class QuestMaker {
    public static final Identifier ID = QuestMod.id("quest_maker");
    
    private int minStart;
    private float minTimeMod;
    private int minLimit;
    private int maxStart;
    private float maxTimeMod;
    private int maxLimit;
    private int midStart;
    private float midTimeMod;
    private int midLimit;
    
    private boolean prioritizeCloser;
    private float threshold;
    
    private float xpMultiplier = 1f;
    private int maxCount = 64;
    private float maxShrink = 0.25f;
    private float maxXpRatio = 1f;
    
    public QuestMaker() {}
    
    public void reload(JsonObject json) {
        JsonObject config = json.getAsJsonObject("config");
        minStart = getJsonInt(config, "min", 0);
        minTimeMod = getJsonFloat(config, "min_time_mod", 0);
        minLimit = getJsonInt(config, "min_limit", 0);
        midStart = getJsonInt(config, "mid", 0);
        midTimeMod = getJsonFloat(config, "mid_time_mod", 0);
        midLimit = getJsonInt(config, "mid_limit", 0);
        maxStart = getJsonInt(config, "max", 1);
        maxTimeMod = getJsonFloat(config, "max_time_mod", 0);
        maxLimit = getJsonInt(config, "max_limit", 0);
        
        JsonObject targetConfig = json.getAsJsonObject("targetConfig");
        prioritizeCloser = getJsonBoolean(targetConfig, "prioritize_closer", true);
        threshold = getJsonFloat(targetConfig, "threshold", 0.1f);
        
        JsonObject rewardConfig = json.getAsJsonObject("rewardConfig");
        xpMultiplier = getJsonFloat(rewardConfig, "xp_multiplier", 1);
        maxCount = getJsonInt(rewardConfig, "max_count", 64);
        maxShrink = getJsonFloat(rewardConfig, "max_shrink", 0.25f);
        maxXpRatio = getJsonFloat(rewardConfig, "max_xp_ratio", 1);
    }
    
    public QuestInstance createNewQuest(MinecraftServer server) {
        boolean enableQuests = server.getGameRules().getBoolean(QuestMod.ENABLE_QUESTS);
        if (!enableQuests) return null;
        // Value selection
        long time = server.getOverworld().getTime();
        int value = getValue(time);
        
        Random random = server.getOverworld().getRandomSequences().getOrCreate(ID);
        if (QUEST_LOADER.questLists == null || QUEST_LOADER.questLists.isEmpty()) return null;
        QuestList list = QuestList.select(QUEST_LOADER.questLists.values(), random);
        if (list == null || list.targets.isEmpty() || list.rewards.isEmpty()) return null;
        
        boolean enableDeathQuests = server.getGameRules().getBoolean(QuestMod.ENABLE_DEATH_QUESTS);
        boolean enablePvpQuests = server.getGameRules().getBoolean(QuestMod.ENABLE_PVP_QUESTS);
        
        QuestTarget target = selectTarget(list, value, random, enableDeathQuests, enablePvpQuests);
        if (target == null) return null;
        QuestReward reward = selectReward(list, value, random);
        
        int id = QuestData.getNextQuestId(server);
        return new QuestInstance(id, target, reward);
    }
    
    private QuestTarget selectTarget(QuestList list, int value, Random random, boolean enableDeathQuests, boolean enablePvpQuests) {
        // Target type selection
        List<QuestList.Target<?>> validTargets = filtered(list.targets, t -> t.base() + t.value() <= value);
        if (validTargets.isEmpty()) return null;
        List<Pair<QuestList.Target<?>, Integer>> targetValues = new ArrayList<>();
        for (QuestList.Target<?> target : validTargets) {
            if (!enableDeathQuests && target.isDeathQuest() || !enablePvpQuests && target.isPvpQuest()) continue;
            int singleValue = target.value();
            int base = target.base();
            int count = (value - base) / singleValue;
            int actualValue = count * singleValue + base;
            Pair<QuestList.Target<?>, Integer> pair = Pair.of(target, actualValue);
            targetValues.add(pair);
        }
        targetValues.sort(Comparator.comparingInt(a -> Math.abs(a.getSecond() - value)));
        Pair<QuestList.Target<?>, Integer> pair;
        QuestList.Target<?> targetType;
        Optional<ScoreboardCriterion> criterion;
        Predicate<Pair<QuestList.Target<?>, Integer>> targetFilter = t -> (float) Math.abs(t.getSecond() - value) / value <= threshold;
        if (!prioritizeCloser && targetValues.stream().anyMatch(targetFilter)) {
            targetValues.removeIf(targetFilter.negate());
        } else {
            int minimumValue = targetValues.getFirst().getSecond();
            targetValues.removeIf(t -> t.getSecond() > minimumValue);
        }
        do {
            pair = getAndRemoveRandom(targetValues, random);
            assert pair != null;
            targetType = pair.getFirst();
            criterion = targetType.stat();
        } while (criterion.isEmpty());
        return new QuestTarget(criterion.get(), pair.getSecond());
    }
    
    private @NotNull QuestReward selectReward(QuestList list, int value, Random random) {
        // Reward selection
        List<QuestList.Reward> validRewards = filtered(list.rewards, t -> t.base() + t.value() <= value);
        QuestList.Reward rewardType = getRandom(validRewards, random);
        int singleRewardValue = rewardType.value();
        int rewardBase = rewardType.base();
        int rewardMaxCount = (value - rewardBase) / singleRewardValue;
        int actualMaxValue = rewardMaxCount * singleRewardValue + rewardBase;
        float a = actualMaxValue * (1 - maxShrink);
        float b = value / (1 + maxXpRatio);
        int minValue = Math.round(Math.max(a, b));
        if (minValue > actualMaxValue) minValue = actualMaxValue;
        
        int rewardValue = random.nextBetween(minValue, actualMaxValue);
        int rewardCount = Math.clamp((rewardValue - rewardBase) / singleRewardValue, 1, maxCount);
        float actualRewardValue = rewardCount * singleRewardValue + rewardBase;
        float xpValue = actualRewardValue <= value ? value - actualRewardValue : 0;
        int xp = (int) Math.ceil(xpValue * xpMultiplier);
        ItemStack stack = new ItemStack(rewardType.item(), rewardCount);
        return new QuestReward(stack, xp);
    }
    
    private int getValue(long time) {
        long timeValue = (long) Math.sqrt(time);
        int min = Math.min(minLimit, (int) (this.minStart + timeValue * minTimeMod));
        int mid = Math.min(midLimit, (int) (this.midStart + timeValue * midTimeMod));
        int max = Math.min(maxLimit, (int) (this.maxStart + timeValue * maxTimeMod));
        return pickTriangular(min, mid, max);
    }
    
    private static int pickTriangular(int min, int mode, int max) {
        if (min == max) return min;
        
        if (min > max) {
            // Switch min and max
            int temp = min;
            min = max;
            max = temp;
        }
        // Make sure mode is between min and max
        mode = Math.clamp(mode, min, max);
        
        // Pick target random value
        double random = Math.random();
        
        int minToMid = mode - min;
        int range = max - min;
        double f = (double) minToMid / (double) range;
        double value;
        if (random < f) {
            double inc = random * range * minToMid;
            value = min + Math.sqrt(inc);
        } else {
            int midToMax = max - mode;
            double dec = (1 - random) * range * midToMax;
            value = max - Math.sqrt(dec);
        }
        
        return (int) Math.round(value);
    }
}
