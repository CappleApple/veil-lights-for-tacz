package com.cappleapple.veiltaczlights.compat.tacz;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.ArrayDeque;
import java.util.Deque;

@EventBusSubscriber(value = Dist.CLIENT, modid = VeilTaczLights.MOD_ID)
public final class RenderOwnerTracker {
    private static final ThreadLocal<Deque<LivingEntity>> OWNERS = ThreadLocal.withInitial(ArrayDeque::new);

    @SubscribeEvent
    public static void beforeLivingRender(RenderLivingEvent.Pre<?, ?> event) {
        OWNERS.get().push(event.getEntity());
    }

    @SubscribeEvent
    public static void afterLivingRender(RenderLivingEvent.Post<?, ?> event) {
        Deque<LivingEntity> owners = OWNERS.get();
        if (!owners.isEmpty()) {
            owners.pop();
        }
        if (owners.isEmpty()) {
            OWNERS.remove();
        }
    }

    public static LivingEntity current() {
        return OWNERS.get().peek();
    }

    private RenderOwnerTracker() {
    }
}
