package com.sindarin.farsightedmobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class Config {
    public static class Server {
        public final ForgeConfigSpec.DoubleValue defaultHostileRange;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> mobAttributeList;
        public Map<EntityType<?>, List<Pair<Attribute, Double>>> mobAttributeMap;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Server configuration settings").push("server");
            this.defaultHostileRange = builder
                    .comment("The minimum follow range for hostile mobs",
                            "Only overridden if the original mob's follow range was lower",
                            "Default value: 32. Vanilla behaviour: 16")
                    .defineInRange("minHostileFollowRange",
                            32, 0.0, 2048.0);
            this.mobAttributeList = builder
                    .comment("A list of follow range overrides. Entry format: \"mod:entity_id,range\"",
                            "Example: [\"minecraft:creeper,32\",\"minecraft:zombie,16\"]",
                            "Default value: []")
                    .defineList("mobFollowRangeOverrides",
                            Collections.emptyList(),
                            o -> o instanceof String);
        }

        public void onLoadConfig() {
            FarsightedMobs.LOGGER.info("Loading config");
            mobAttributeMap = new HashMap<>();
            Map<EntityType<?>, Pair<Attribute, Double>> followRanges = parseMobAttributes(mobAttributeList.get(), Attributes.FOLLOW_RANGE, 0, 2048);
            for (Map.Entry<EntityType<?>, Pair<Attribute, Double>> attribute : followRanges.entrySet()) {
                // Map already contains this mob. Add the extra attribute
                if (!mobAttributeMap.containsKey(attribute.getKey())) {
                    mobAttributeMap.put(attribute.getKey(), new ArrayList<>());
                }
                mobAttributeMap.get(attribute.getKey()).add(attribute.getValue());
            }
        }
    }
    static final ForgeConfigSpec serverSpec;
    public static final Config.Server SERVER;
    static {
        final Pair<Server, ForgeConfigSpec> serverPair =
                new ForgeConfigSpec.Builder().configure(Config.Server::new);
        serverSpec = serverPair.getRight();
        SERVER = serverPair.getLeft();
    }

    public static Map<EntityType<?>, Pair<Attribute, Double>> parseMobAttributes(
            List<? extends String> unparsed,
            Attribute attribute,
            double minValue,
            double maxValue
    ) {
        Map<EntityType<?>, Pair<Attribute, Double>> map = new HashMap<>();
        for (String line : unparsed) {
            String[] split = line.split(",");
            if (split.length != 2) {
                FarsightedMobs.LOGGER.warn("Invalid line in server config: " + line);
                continue;
            }

            // Find entity
            ResourceLocation entityName = ResourceLocation.tryParse(split[0]);
            if (entityName == null) {
                FarsightedMobs.LOGGER.warn("Invalid resource location in server config: " + line);
                continue;
            }
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityName)) {
                FarsightedMobs.LOGGER.warn("Could not find entity: " + line);
                continue;
            }
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityName);

            // Parse value
            if (!NumberUtils.isParsable(split[1])) {
                FarsightedMobs.LOGGER.warn("Invalid number for attribute: " + line);
                continue;
            }
            double value = Double.parseDouble(split[1]);
            if (value > maxValue) {
                FarsightedMobs.LOGGER.warn("Number is too big for attribute: " + line);
                continue;
            }
            if (value < minValue) {
                FarsightedMobs.LOGGER.warn("Number is too small for attribute: " + line);
                continue;
            }


            // Create return value
            map.put(entityType, Pair.of(attribute, value));
        }
        return map;
    }

    @SubscribeEvent
    public static void onModConfigEvent(final ModConfigEvent event) {
        if (!event.getConfig().getModId().equals(FarsightedMobs.MOD_ID)) return;
        Config.SERVER.onLoadConfig();
    }
}
