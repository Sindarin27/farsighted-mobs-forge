package net.sindarin27.farsightedmobs;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(FarsightedMobs.MODID)
public class FarsightedMobs
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "farsightedmobs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public FarsightedMobs(IEventBus modEventBus)
    {
        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
        
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    @SubscribeEvent
    public void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Trust the server on this one
        if (event.getLevel().isClientSide()) return;

        // Get the entity
        Entity entity = event.getEntity();

        // Check for living entity stuff
        if (!(entity instanceof LivingEntity livingEntity)) return;

        // If monster, update the follow range
        if (livingEntity instanceof Monster mob) {
            // But only when the new value is bigger than the old
            double originalFollow = mob.getAttributeBaseValue(Attributes.FOLLOW_RANGE);
            if (originalFollow < Config.defaultHostileRange) {
                ChangeBaseAttributeValue(mob, Attributes.FOLLOW_RANGE, Config.defaultHostileRange);
            }
        }

        // Get type and find attributes to change
        EntityType<?> type = livingEntity.getType();
        if (Config.mobAttributeMap.containsKey(type)) {
            List<Pair<Attribute, Double>> values = Config.mobAttributeMap.get(type);

            // Change attributes
            for (Pair<Attribute, Double> change : values) {
                ChangeBaseAttributeValue(livingEntity, change.getLeft(), change.getRight());
            }
        }
        // Fix the minecraft bug that causes entities to never update their follow range by updating it once they spawn
        FixFollowRange(livingEntity);
    }

    // Change the base value of the given attribute on the given entity to the given value
    private static void ChangeBaseAttributeValue(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        // Safety goes first
        if (attributeInstance == null) {
            LOGGER.warn("No attribute instance found for " + attribute.getDescriptionId());
            return;
        }
        attributeInstance.setBaseValue(value);
    }

    private static void FixFollowRange(LivingEntity livingEntity) {
        if (livingEntity instanceof Mob mob) {
            mob.targetSelector.getAvailableGoals().forEach(wrappedGoal -> {
                Goal goal = wrappedGoal.getGoal();
                if (goal instanceof NearestAttackableTargetGoal natGoal) {
                    natGoal.targetConditions = natGoal.targetConditions.range(livingEntity.getAttributeValue(Attributes.FOLLOW_RANGE));
                }
            });
        }
    }
}
