package net.sindarin27.farsightedmobs;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(FarsightedMobs.MOD_ID)
// Register ourselves for game events we are interested in
@EventBusSubscriber(modid = FarsightedMobs.MOD_ID)
public class FarsightedMobs {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "farsightedmobs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public FarsightedMobs(ModContainer container) {
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us

        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Trust the server on this one
        if (event.getLevel().isClientSide()) return;

        // Get the entity
        Mob mob = event.getEntity();

        // If monster, update the follow range
        if (mob instanceof Monster monster) {
            // But only when the new value is bigger than the old
            double originalFollow = monster.getAttributeBaseValue(Attributes.FOLLOW_RANGE);
            if (originalFollow < Config.defaultHostileRange) {
                ChangeBaseAttributeValue(monster, Attributes.FOLLOW_RANGE, Config.defaultHostileRange);
            }
        }

        // Get type and find attributes to change
        EntityType<?> type = mob.getType();
        if (Config.mobAttributeMap.containsKey(type)) {
            List<Pair<Holder<Attribute>, Double>> values = Config.mobAttributeMap.get(type);

            // Change attributes
            for (Pair<Holder<Attribute>, Double> change : values) {
                ChangeBaseAttributeValue(mob, change.getLeft(), change.getRight());
            }
        }
        // Fix the minecraft bug that causes entities to never update their follow range by updating it once they spawn
        FixFollowRange(mob);
    }

    // Change the base value of the given attribute on the given entity to the given value
    private static void ChangeBaseAttributeValue(Mob mob, Holder<Attribute> attribute, double value) {
        AttributeInstance attributeInstance = mob.getAttribute(attribute);
        // Safety goes first
        if (attributeInstance == null) {
            LOGGER.warn("No attribute instance found for " + attribute.value().getDescriptionId());
            return;
        }
        attributeInstance.setBaseValue(value);
    }

    private static void FixFollowRange(Mob mob) {
        mob.targetSelector.getAvailableGoals().forEach(wrappedGoal -> {
            Goal goal = wrappedGoal.getGoal();
            if (goal instanceof NearestAttackableTargetGoal<?> natGoal) {
                natGoal.targetConditions = natGoal.targetConditions.range(mob.getAttributeValue(Attributes.FOLLOW_RANGE));
            }
        });
    }
}
