package net.sindarin27.farsightedmobs;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.*;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@EventBusSubscriber(modid = FarsightedMobs.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.DoubleValue DEFAULT_HOSTILE_RANGE = BUILDER
            .comment("The minimum follow range for hostile mobs",
                    "Only overridden if the original mob's follow range was lower",
                    "Default value: 32. Vanilla behaviour: 16")
            .defineInRange("minHostileFollowRange",
                    32, 0.0, 2048.0);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_ATTRIBUTE_LIST = BUILDER
            .comment("A list of follow range overrides. Entry format: \"mod:entity_id,range\"",
                    "Example: [\"minecraft:creeper,32\",\"minecraft:zombie,16\"]",
                    "Default value: []")
            .defineListAllowEmpty("mobFollowRangeOverrides",
                    Collections.emptyList(),
                    Config::validateEntityNameWithValue);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double defaultHostileRange;
    public static Map<EntityType<?>, List<Pair<Holder<Attribute>, Double>>> mobAttributeMap;

    private static boolean validateEntityNameWithValue(final Object obj)
    {
        if (obj instanceof  String entityNameWithValue) {
            String[] nameAndValue = entityNameWithValue.split(",");
            if (nameAndValue.length != 2) return false;
            return BuiltInRegistries.ENTITY_TYPE.containsKey(new ResourceLocation(nameAndValue[0]));
        }
        else return false;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        LOGGER.info("Loading config");
        defaultHostileRange = DEFAULT_HOSTILE_RANGE.get();
        mobAttributeMap = new HashMap<>();
        Map<EntityType<?>, Pair<Holder<Attribute>, Double>> followRanges = 
                parseMobAttributes(MOB_ATTRIBUTE_LIST.get(), Attributes.FOLLOW_RANGE, 0, 2048);
        for (Map.Entry<EntityType<?>, Pair<Holder<Attribute>, Double>> attribute : followRanges.entrySet()) {
            // Map already contains this mob. Add the extra attribute
            if (!mobAttributeMap.containsKey(attribute.getKey())) {
                mobAttributeMap.put(attribute.getKey(), new ArrayList<>());
            }
            mobAttributeMap.get(attribute.getKey()).add(attribute.getValue());
        }
    }

    public static Map<EntityType<?>, Pair<Holder<Attribute>, Double>> parseMobAttributes(
            List<? extends String> unparsed,
            Holder<Attribute> attribute,
            double minValue,
            double maxValue
    ) {
        Map<EntityType<?>, Pair<Holder<Attribute>, Double>> map = new HashMap<>();
        for (String line : unparsed) {
            String[] split = line.split(",");
            if (split.length != 2) {
                LOGGER.warn("Invalid line in server config: " + line);
                continue;
            }

            // Find entity
            ResourceLocation entityName = ResourceLocation.tryParse(split[0]);
            if (entityName == null) {
                LOGGER.warn("Invalid resource location in server config: " + line);
                continue;
            }
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityName)) {
                LOGGER.warn("Could not find entity: " + line);
                continue;
            }
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityName);

            // Parse value
            if (!NumberUtils.isParsable(split[1])) {
                LOGGER.warn("Invalid number for attribute: " + line);
                continue;
            }
            double value = Double.parseDouble(split[1]);
            if (value > maxValue) {
                LOGGER.warn("Number is too big for attribute: " + line);
                continue;
            }
            if (value < minValue) {
                LOGGER.warn("Number is too small for attribute: " + line);
                continue;
            }


            // Create return value
            map.put(entityType, Pair.of(attribute, value));
        }
        return map;
    }
}
