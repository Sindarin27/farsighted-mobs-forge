package com.sindarin.farsightedmobs;

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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod(FarsightedMobs.MOD_ID)
public class FarsightedMobs {
    public static final String MOD_ID = "farsightedmobs";
    public static final Logger LOGGER = LogManager.getLogger(FarsightedMobs.MOD_ID);
    public FarsightedMobs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().register(Config.class);
    }

    @SubscribeEvent
    public void onMobSpawn(EntityJoinLevelEvent event) {
        // Trust the server on this one
        if (event.getLevel().isClientSide) return;

        // Get the entity
        Entity entity = event.getEntity();

        // Check for living entity stuff
        if (!(entity instanceof LivingEntity livingEntity)) return;

        // If monster, update the follow range
        if (livingEntity instanceof Monster mob) {
            // But only when the new value is bigger than the old
            double originalFollow = mob.getAttributeBaseValue(Attributes.FOLLOW_RANGE);
            if (originalFollow < Config.SERVER.defaultHostileRange.get()) {
                ChangeBaseAttributeValue(mob, Attributes.FOLLOW_RANGE, Config.SERVER.defaultHostileRange.get());
            }
        }

        // Get type and find attributes to change
        EntityType<?> type = livingEntity.getType();
        if (Config.SERVER.mobAttributeMap.containsKey(type)) {
            List<Pair<Attribute, Double>> values = Config.SERVER.mobAttributeMap.get(type);

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
