package com.sindarin.farsightedmobs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
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
    public void onMobSpawn(EntityJoinWorldEvent event) {
        // Trust the server on this one
        if (event.getWorld().isClientSide) return;

        // Get the entity
        Entity entity = event.getEntity();

        // Check for living entity stuff
        if (!(entity instanceof LivingEntity)) return;
        LivingEntity livingEntity = (LivingEntity)entity;

        // If monster, update the follow range
        if (livingEntity instanceof MonsterEntity) {
            MonsterEntity mob = (MonsterEntity) livingEntity;
            // But only when the new value is bigger than the old
            double originalFollow = mob.getAttributeBaseValue(Attributes.FOLLOW_RANGE);
            if (originalFollow < Config.SERVER.defaultHostileRange.get()) {
                ChangeBaseAttributeValue(mob, Attributes.FOLLOW_RANGE, Config.SERVER.defaultHostileRange.get());
            }
        }

        // Get type and find attributes to change
        EntityType<?> type = livingEntity.getType();
        if (!Config.SERVER.mobAttributeMap.containsKey(type)) return;
        List<Pair<Attribute, Double>> values = Config.SERVER.mobAttributeMap.get(type);

        // Change attributes
        for (Pair<Attribute, Double> change : values) {
            ChangeBaseAttributeValue(livingEntity, change.getLeft(), change.getRight());
        }
    }

    // Change the base value of the given attribute on the given entity to the given value
    private static void ChangeBaseAttributeValue(LivingEntity entity, Attribute attribute, double value) {
        ModifiableAttributeInstance attributeInstance = entity.getAttribute(attribute);
        // Safety goes first
        if (attributeInstance == null) {
            LOGGER.warn("No attribute instance found for " + attribute.getRegistryName());
            return;
        }
        attributeInstance.setBaseValue(value);
    }
}
