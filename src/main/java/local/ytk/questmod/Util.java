package local.ytk.questmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import local.ytk.questmod.quests.QuestInstance;
import local.ytk.questmod.quests.QuestReward;
import local.ytk.questmod.quests.QuestTarget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    
    public static int getJsonInt(JsonObject json, String key, int defaultValue) {
        return json.get(key) instanceof JsonPrimitive p && p.isNumber() ? p.getAsInt() : defaultValue;
    }
    public static float getJsonFloat(JsonObject json, String key, float defaultValue) {
        return json.get(key) instanceof JsonPrimitive p && p.isNumber() ? p.getAsFloat() : defaultValue;
    }
    public static boolean getJsonBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.get(key) instanceof JsonPrimitive p && p.isBoolean() ? p.getAsBoolean() : defaultValue;
    }
    public static String getJsonString(JsonObject json, String key, String defaultValue) {
        return json.get(key) instanceof JsonPrimitive p ? p.getAsString() : defaultValue;
    }
    
    public static <T> T getAndRemoveRandom(List<T> list, Random random) {
        if (list.isEmpty()) return null;
        int index = random.nextInt(list.size());
        return list.remove(index);
    }
    public static <T> T getWeightedRandom(List<T> list, ToIntFunction<T> weightGetter, Random random) {
        int[] weights = new int[list.size()];
        weights[0] = weightGetter.applyAsInt(list.getFirst());
        for (int i = 1; i < weights.length; i++) weights[i] = weights[i - 1] + weightGetter.applyAsInt(list.get(i));
        int n = random.nextInt(weights[weights.length - 1]);
        for (int i = 0; i < weights.length; i++) if (n < weights[i]) return list.get(i);
        return list.getLast();
    }
    public static <T> Optional<T> cast(Object value, Class<T> type) {
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }
    public static <T> Stream<T> castToStream(Object value, Class<T> type) {
        return type.isInstance(value) ? Stream.of(type.cast(value)) : Stream.empty();
    }
    
    public static <T> @Nullable T castOrNull(Object value, Class<T> type) {
        return type.isInstance(value) ? type.cast(value) : null;
    }
    
    public static <T> Predicate<T> filterEqual(T other) {
        return t -> Objects.equals(t, other);
    }
    
    public static <T> List<T> filtered(Collection<T> collection, Predicate<T> filter) {
        return collection.stream().filter(filter).toList();
    }
    
    public static <T> Text getStatName(Stat<T> stat, int count) {
        StatType<T> type = stat.getType();
        Identifier typeId = Registries.STAT_TYPE.getId(type);
        if (typeId == null) throw new IllegalStateException("Stat target '" + type.getName().getString() + "' has no ID");
        
        if (type.equals(Stats.CUSTOM)) {
            Identifier statId = (Identifier) stat.getValue();
            return Text.translatable(statId.toTranslationKey("quest.custom"), count);
        }
        String typeKey = typeId.toTranslationKey("quest.target");
        Registry<T> registry = type.getRegistry();
        String prefix = registry.getKey().getValue().getPath();
        if (Objects.equals(prefix, "entity_type")) prefix = "entity";
        Identifier value = registry.getId(stat.getValue());
        assert value != null;
        String valueKey = value.toTranslationKey(prefix);
        Text valueText = Text.translatable(valueKey);
        return Text.translatable(typeKey, valueText, count);
    }
    
    public static @NotNull Text getScoreName(ScoreboardCriterion score, int count) {
        if (score instanceof Stat<?> stat) return getStatName(stat, count);
        String name = score.getName();
        String key = "quest.score." + name;
        return Text.translatable(key, count);
    }
    
    public static @NotNull MutableText getQuestName(QuestInstance quest) {
        QuestTarget target = quest.target();
        Text targetText = getScoreName(target.score(), target.count());
        QuestReward reward = quest.reward();
        ItemStack rewardItem = reward.item();
        Text rewardText = Text.translatable("commands.quest.reward", rewardItem.getCount(), rewardItem.toHoverableText(), reward.xp());
        return Text.translatable("commands.quest.get", quest.id(), targetText, rewardText);
    }
}
