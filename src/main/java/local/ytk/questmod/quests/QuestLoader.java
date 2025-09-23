package local.ytk.questmod.quests;

import com.google.gson.*;
import local.ytk.questmod.QuestMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static local.ytk.questmod.Util.*;

public class QuestLoader implements SimpleSynchronousResourceReloadListener {
    public static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestLoader.class);
    public static final Identifier DEFAULT = QuestMod.id("quests/default.json");
    public static Identifier ID = QuestMod.id("quests");
    Map<Identifier, QuestList> questLists;
    public final QuestMaker questMaker = new QuestMaker();
    
    public QuestLoader() {}
    
    @Override
    public Identifier getFabricId() {
        return ID;
    }
    @Override
    public void reload(ResourceManager manager) {
        Map<Identifier, Resource> resources = manager.findResources("quests", path -> path.getPath().endsWith(".json"));
        this.questLists = resources
                .entrySet()
                .stream()
                .map(entry -> loadQuestList(entry.getKey(), entry.getValue())
                        .map(questList -> Map.entry(entry.getKey(), questList)))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    private Optional<QuestList> loadQuestList(Identifier id, Resource resource) {
        QuestList questList = new QuestList(id);
        try (InputStream stream = resource.getInputStream()) {
            String content = new String(stream.readAllBytes());
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            
            if (json.has("weight")) questList.weight = getJsonInt(json, "weight", 1);
            
            JsonObject types = json.getAsJsonObject("types");
            
            loadQuestTypes(questList, types);
            loadDefaults(questList, types, json);
            
            JsonArray rewardsArray = json.getAsJsonArray("rewards");
            for (JsonElement e : rewardsArray.asList()) if (e instanceof JsonObject o) createReward(questList, o);
            if (id.equals(DEFAULT)) questMaker.reload(json);
        } catch (IOException | JsonSyntaxException | ClassCastException e) {
            return Optional.empty();
        }
        return Optional.of(questList);
    }
    
    private static void createReward(QuestList questList, JsonObject obj) {
        String itemId = getJsonString(obj, "id", null);
        if (itemId == null) return;
        int value = getJsonInt(obj, "value", -1);
        if (value < 0) return;
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null) return;
        Optional<RegistryEntry.Reference<Item>> item = Registries.ITEM.getEntry(identifier);
        if (item.isEmpty()) return;
        int base = getJsonInt(obj, "base", 0);
        QuestList.Reward reward = new QuestList.Reward(questList, item.get().value(), base, value);
        questList.addReward(reward);
    }
    
    private static void loadDefaults(QuestList questList, JsonObject types, JsonObject json) {
        createBlockList(questList, Stats.MINED, QuestList.BlockTarget::isMineable);
        createItemList(questList, Stats.CRAFTED, QuestList.ItemTarget::isCraftable);
        createItemList(questList, Stats.USED, QuestList.ItemTarget::isUsable);
        
        JsonObject killed = types.getAsJsonObject("minecraft:killed");
        if (killed != null) createEntityList(questList, Stats.KILLED,
                getJsonInt(killed, "defaultBase", 0),
                getJsonInt(killed, "default", 0),
                killed.getAsJsonObject("overrides"));
        JsonObject killedBy = types.getAsJsonObject("minecraft:killed_by");
        if (killedBy != null) createEntityList(questList, Stats.KILLED_BY,
                getJsonInt(killedBy, "defaultBase", 0),
                getJsonInt(killedBy, "default", 0),
                killedBy.getAsJsonObject("overrides"), QuestList.EntityTarget::canAttackPlayer);
    }
    
    private static void loadQuestTypes(QuestList list, JsonObject json) {
        json.asMap().forEach((k, v) -> {
            if (!(v instanceof JsonObject obj)) return;
            switch (k) {
                case "*mined" -> {
                    int noTool = obj.get("noTool").getAsInt();
                    int wood = obj.get("wood").getAsInt();
                    int stone = obj.get("stone").getAsInt();
                    int iron = obj.get("iron").getAsInt();
                    int diamond = obj.get("diamond").getAsInt();
                    int value = obj.get("value").getAsInt();
                    float hardnessMultiplier = obj.get("hardnessMultiplier").getAsFloat();
                    list.blockModifiers = new QuestList.BlockModifiers(noTool, wood, stone, iron, diamond, value, hardnessMultiplier);
                }
                case "*crafted" -> {
                    int commonBase = obj.get("commonBase").getAsInt();
                    int common = obj.get("common").getAsInt();
                    int uncommonBase = obj.get("uncommonBase").getAsInt();
                    int uncommon = obj.get("uncommon").getAsInt();
                    int rareBase = obj.get("rareBase").getAsInt();
                    int rare = obj.get("rare").getAsInt();
                    int epicBase = obj.get("epicBase").getAsInt();
                    int epic = obj.get("epic").getAsInt();
                    list.itemModifiers = new QuestList.ItemModifiers(commonBase, common, uncommonBase, uncommon, rareBase, rare, epicBase, epic);
                }
                case null, default -> {
                    loadStatTypeList(list, k, obj);
                }
            }
        });
    }
    public static void loadStatTypeList(QuestList list, String key, JsonObject json) {
        if (!json.has("type")) {
            Optional<ScoreboardCriterion> criterion = ScoreboardCriterion.getOrCreateStatCriterion(key);
            if (criterion.isEmpty()) {
                LOGGER.warn("Criterion not found for key '{}'", key);
                return;
            }
            QuestList.SimpleTarget target = new QuestList.SimpleTarget(
                    list, criterion.get(),
                    getJsonInt(json, "base", 0),
                    getJsonInt(json, "value", 0),
                    getJsonBoolean(json, "death", false),
                    getJsonBoolean(json, "pvp", false)
            );
            list.addTarget(target);
            return;
        }
        StatType<?> statType = Registries.STAT_TYPE.get(Identifier.of(key));
        if (json.has("overrides")) {
//            String target = json.get("target") instanceof JsonPrimitive p ? p.getAsString() : null;
//            switch (target) {
//                case "item" -> createList(list, statType, QuestList.ItemTarget::new);
//                case "block" -> createList(list, statType, QuestList.BlockTarget::new);
//                case "entity_type" -> createEntityList(list, statType, ));
//            }
            // Handled separately
            return;
        }
        JsonObject values = json.getAsJsonObject("values");
        if (statType == null) {
            LOGGER.warn("Stat target not found for key '{}'", key);
            return;
        }
        values.asMap().forEach((k, v) -> {
            if (!(v instanceof JsonObject obj)) return;
            Object value = statType.getRegistry().get(Identifier.of(k));
            list.targets.add(new QuestList.BaseTarget<>(
                    list, (StatType<Object>) statType, value,
                    getJsonInt(obj, "base", Integer.MIN_VALUE),
                    getJsonInt(obj, "value", Integer.MIN_VALUE),
                    getJsonBoolean(obj, "death", false),
                    getJsonBoolean(obj, "pvp", false)
            ));
        });
    }
    
    private static <T> void loadQuestType(QuestList list, StatType<T> statType, Identifier id, JsonObject json) {
        T statValue = statType.getRegistry().get(id);
        if (statValue == null) {
            LOGGER.warn("Stat value '{}' not in registry '{}' for target '{}'", id, statType.getRegistry().getKey(), statType.getName().getString());
            return;
        }
        QuestList.BaseTarget<T> type = new QuestList.BaseTarget<>(
                list, statType, statValue,
                getJsonInt(json, "base", 0),
                getJsonInt(json, "value", 0),
                getJsonBoolean(json, "death", false),
                getJsonBoolean(json, "pvp", false)
        );
        list.addTarget(type);
    }
    public static void createBlockList(QuestList list, StatType<Block> statType) {
        createList(list, statType, QuestList.BlockTarget::new);
    }
    public static void createItemList(QuestList list, StatType<Item> statType) {
        createList(list, statType, QuestList.ItemTarget::new);
    }
    public static void createBlockList(QuestList list, StatType<Block> statType, Predicate<Block> filter) {
        createList(list, statType, QuestList.BlockTarget::new, filter);
    }
    public static void createItemList(QuestList list, StatType<Item> statType, Predicate<Item> filter) {
        createList(list, statType, QuestList.ItemTarget::new, filter);
    }
    public static void createEntityList(QuestList list, StatType<EntityType<?>> statType, int defaultBase, int defaultValue, JsonObject overrides) {
        createEntityList(list, statType, defaultBase, defaultValue, overrides, null);
    }
    public static void createEntityList(QuestList list, StatType<EntityType<?>> statType, int defaultBase, int defaultValue, JsonObject overrides, Predicate<EntityType<?>> filter) {
        createList(list, statType, (l, t, v) -> {
            Identifier id = QuestList.TargetFactory.getId(t, v);
            int base = defaultBase;
            int value = defaultValue;
            if (overrides.has(id.toString())) {
                if (overrides.get("exclude") instanceof JsonPrimitive p && p.isBoolean() && p.getAsBoolean()) return null;
                if (overrides.get("base") instanceof JsonPrimitive p && p.isNumber()) base = p.getAsInt();
                if (overrides.get("value") instanceof JsonPrimitive p && p.isNumber()) value = p.getAsInt();
            }
            return new QuestList.EntityTarget(l, t, v, base, value);
        }, filter);
    }
    
    public static <T> void createList(QuestList list, StatType<T> statType, QuestList.TargetFactory<T> factory) {
        Registry<T> registry = statType.getRegistry();
        registry.forEach(value -> {
            QuestList.Target<T> result = factory.create(list, statType, value);
            if (result == null) return;
            list.addTarget(result);
        });
    }
    public static <T> void createList(QuestList list, StatType<T> statType, QuestList.TargetFactory<T> factory, Predicate<T> filter) {
        if (filter == null) {
            createList(list, statType, factory);
            return;
        }
        Registry<T> registry = statType.getRegistry();
        registry.forEach(value -> {
            if (!filter.test(value)) return;
            QuestList.Target<T> result = factory.create(list, statType, value);
            if (result == null) return;
            list.addTarget(result);
        });
    }
    
    public Map<Identifier, QuestList> questLists() {
        return questLists;
    }
}
