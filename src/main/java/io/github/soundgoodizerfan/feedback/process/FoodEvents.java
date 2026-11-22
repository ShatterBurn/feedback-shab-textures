// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.prep.DishBake;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Where food starts to spoil and where it is judged (food spec 4.2, 8). Nothing here ticks an item.
 *
 * <p>A food that has never been stamped is timeless, so a dropped food (a mob's drop, a harvest, a
 * thrown item) is stamped as it enters the world. Food generated straight into a chest or a pocket
 * is stamped by the first machine that touches it ({@link Cooking#tick}, the ice box, the rack).
 * Eating advances the spoilage first, so what you eat is what it has become.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FoodEvents {

    private FoodEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ItemEntity item))
            return;
        ItemStack stack = item.getItem();
        if (stack.has(FDataComponents.FOOD_STATE.get())) {
            // A dish dropped before any pack read it starts its clock here.
            if (stack.has(FDataComponents.DISH_FIGURES.get()))
                Cooking.settle(stack, event.getLevel(), AmbientTable.at(event.getLevel()).value());
            return;
        }
        Cooking.find(event.getLevel(), stack).ifPresent(entry -> {
            FoodState fresh = Cooking.fresh(entry, event.getLevel());
            if (stack.is(Tags.Items.EGGS)) {
                // Disease in a flock follows crowding: a lone free-range bird lays near-clean, a pen does not.
                int birds = Math.max(1, event.getLevel().getEntitiesOfClass(Chicken.class, item.getBoundingBox().inflate(ProcessTuning.EGG_CROWD_RADIUS)).size());
                float pathogen = Math.min(ProcessTuning.EGG_CROWD_PATHOGEN_MAX, ProcessTuning.EGG_FREE_RANGE_PATHOGEN + ProcessTuning.EGG_CROWD_PATHOGEN * (birds - 1));
                fresh = new FoodState(fresh.cook(), fresh.moisture(), pathogen, fresh.toxin(), fresh.charred(), fresh.rot(), fresh.stamp(),
                        fresh.envTu(), fresh.smoke(), fresh.salt());
            }
            stack.set(FDataComponents.FOOD_STATE.get(), fresh);
            Cooking.apply(stack, entry, fresh);
        });
    }

    /** A vanilla egg is thrown, not eaten; give it a small food value so the raw egg can be eaten (its risk is the material's pathogen). */
    @SubscribeEvent
    public static void eggFood(ModifyDefaultComponentsEvent event) {
        event.modify(Items.EGG, b -> b.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).build()));
    }

    /** Sneak and use an egg to eat it; a plain use still throws it. A tap while sneaking is the nibble as for any food. */
    @SubscribeEvent
    public static void onEatEgg(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        var player = event.getEntity();
        if (!stack.is(Tags.Items.EGGS) || !player.isShiftKeyDown() || !stack.has(DataComponents.FOOD) || !player.canEat(false))
            return;
        player.startUsingItem(event.getHand());
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    /**
     * Combine two stacks of one food by hand (cursor onto slot): their states differ in small ways (a few minutes' stamp, a
     * different rot), so vanilla refuses. Both are brought to now, then each figure takes the lower of the two, and vanilla's
     * own merge follows because the stacks are now equal. Only done when the slot has room and takes it; a stack whose other
     * components differ (cut, knead, dish) still does not merge. Shift-click and ground merges are untouched.
     */
    @SubscribeEvent
    public static void onCombine(ItemStackedOnOtherEvent event) {
        ItemStack carried = event.getCarriedItem();
        ItemStack onto = event.getStackedOnItem();
        var level = event.getPlayer().level();
        if (carried.isEmpty() || onto.isEmpty() || carried.getItem() != onto.getItem() || onto.getCount() >= onto.getMaxStackSize()
                || ItemStack.isSameItemSameComponents(carried, onto) || !event.getSlot().mayPlace(carried))
            return;
        // One side may never have been stamped (loot, a chest): settle stamps it, so it merges like the rest.
        var entry = Cooking.findPerishable(level, onto);
        if (entry.isEmpty() || Cooking.findPerishable(level, carried).isEmpty())
            return;
        float env = AmbientTable.at(level).value();
        Cooking.settle(carried, level, env);
        Cooking.settle(onto, level, env);
        FoodState a = carried.get(FDataComponents.FOOD_STATE.get());
        FoodState b = onto.get(FDataComponents.FOOD_STATE.get());
        FoodState low = lowest(a, b);
        carried.set(FDataComponents.FOOD_STATE.get(), low);
        onto.set(FDataComponents.FOOD_STATE.get(), low);
        Cooking.apply(carried, entry.get(), low);
        Cooking.apply(onto, entry.get(), low);
    }

    /** Each figure the lower of the two: combining foods takes the best of both. */
    static FoodState lowest(FoodState a, FoodState b) {
        return new FoodState(Math.min(a.cook(), b.cook()), Math.min(a.moisture(), b.moisture()), Math.min(a.pathogen(), b.pathogen()),
                Math.min(a.toxin(), b.toxin()), Math.min(a.charred(), b.charred()), Math.min(a.rot(), b.rot()), Math.max(a.stamp(), b.stamp()),
                Math.min(a.envTu(), b.envTu()), Math.min(a.smoke(), b.smoke()), Math.min(a.salt(), b.salt()));
    }

    /**
     * Stack {@code incoming} onto {@code existing} when they are one food a little apart: the same item, every component alike
     * but the food's state, the eating figures and name derived from it, and the heat stamp, and every state figure within
     * {@link ProcessTuning#FOOD_MERGE_TOLERANCE}. Both are brought to now, the lower of each figure is kept (as by hand), and the
     * counts add. Returns whether it merged; on false nothing is changed. A machine calls it when a process finishes beside a
     * stack of the same food, so dips and cooking do not leave a row of near-identical stacks.
     */
    public static boolean mergeNear(ItemStack existing, ItemStack incoming, net.minecraft.world.level.Level level, float envTu) {
        if (existing.isEmpty() || incoming.isEmpty() || existing.getItem() != incoming.getItem()
                || existing.getCount() + incoming.getCount() > existing.getMaxStackSize())
            return false;
        var entry = Cooking.findPerishable(level, existing);
        if (entry.isEmpty() || Cooking.findPerishable(level, incoming).isEmpty() || !sameApartFromState(existing, incoming))
            return false;
        ItemStack a = existing.copy();
        ItemStack b = incoming.copy();
        Cooking.settle(a, level, envTu);
        Cooking.settle(b, level, envTu);
        FoodState x = a.get(FDataComponents.FOOD_STATE.get());
        FoodState y = b.get(FDataComponents.FOOD_STATE.get());
        if (x == null || y == null || !close(x, y))
            return false;
        FoodState low = lowest(x, y);
        existing.set(FDataComponents.FOOD_STATE.get(), low);
        Cooking.apply(existing, entry.get(), low);
        existing.grow(incoming.getCount());
        return true;
    }

    private static boolean close(FoodState a, FoodState b) {
        float t = ProcessTuning.FOOD_MERGE_TOLERANCE;
        return Math.abs(a.cook() - b.cook()) <= t && Math.abs(a.moisture() - b.moisture()) <= t && Math.abs(a.pathogen() - b.pathogen()) <= t
                && Math.abs(a.toxin() - b.toxin()) <= t && Math.abs(a.charred() - b.charred()) <= t && Math.abs(a.rot() - b.rot()) <= t
                && Math.abs(a.smoke() - b.smoke()) <= t && Math.abs(a.salt() - b.salt()) <= t;
    }

    private static boolean sameApartFromState(ItemStack a, ItemStack b) {
        ItemStack x = a.copyWithCount(1);
        ItemStack y = b.copyWithCount(1);
        for (ItemStack s : new ItemStack[] {x, y}) {
            s.remove(FDataComponents.FOOD_STATE.get());
            s.remove(DataComponents.FOOD);
            s.remove(DataComponents.ITEM_NAME);
            s.remove(DataComponents.CUSTOM_MODEL_DATA);
            s.remove(FDataComponents.TEMPERATURE.get());
            s.remove(FDataComponents.HEATED_AT.get());
        }
        return ItemStack.isSameItemSameComponents(x, y);
    }

    /** A dish picked up is carried: if it is overfull it spills (dish spec 2.2). Once per pickup, nothing ticked. */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!event.getPlayer().level().isClientSide)
            DishBake.carry(event.getItemEntity().getItem());
    }

    /** A dish thrown down is carried too. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (!event.getPlayer().level().isClientSide)
            DishBake.carry(event.getEntity().getItem());
    }

    /** A dish thrown out of a dead player's pack spills as one tossed down does (death drops fire no toss event). */
    @SubscribeEvent
    public static void onDeathDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player && !player.level().isClientSide)
            event.getDrops().forEach(drop -> DishBake.carry(drop.getItem()));
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Start event) {
        var level = event.getEntity().level();
        ItemStack stack = event.getItem();
        if (!level.isClientSide && stack.has(FDataComponents.CANNED.get())) {
            Canning.settle(stack, level);
            return;
        }
        if (level.isClientSide || !stack.has(FDataComponents.FOOD_STATE.get()))
            return;
        Cooking.findPerishable(level, stack).ifPresent(entry -> {
            FoodState now = Cooking.current(stack, entry, level, AmbientTable.at(level).value());
            stack.set(FDataComponents.FOOD_STATE.get(), now);
            Cooking.apply(stack, entry, now);
        });
    }

    /**
     * The bite (dish spec 5): sneak and use a food to take a nibble. It applies the food's hazard effects as they are now, at
     * {@link ProcessTuning#BITE_SHARE} of the chance and duration, and says which class they are in words. It eats nothing and never
     * refuses; the item's own cooldown keeps it from being spammed, and it stands in for eating while sneaking.
     */
    /**
     * Sneak and tap: a nibble. Sneak and hold eats as usual. Vanilla starts eating on the press, so a release within
     * {@link ProcessTuning#BITE_TAP_TICKS} of starting is the tap ({@link #onTap}); when the player cannot eat now (full) there is
     * no eating to start, so the press itself is the nibble.
     */
    @SubscribeEvent
    public static void onBite(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        var food = stack.get(DataComponents.FOOD);
        if (!event.getEntity().isShiftKeyDown() || food == null || event.getEntity().canEat(food.canAlwaysEat()))
            return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player)
            bite(player, stack);
    }

    @SubscribeEvent
    public static void onTap(LivingEntityUseItemEvent.Stop event) {
        ItemStack stack = event.getItem();
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !stack.has(DataComponents.FOOD) || stack.getUseDuration(player) - event.getDuration() > ProcessTuning.BITE_TAP_TICKS)
            return;
        bite(player, stack);
    }

    /**
     * Hardiness: eating risky food. Vanilla has applied the food's effects by the time this fires; a hazard effect that
     * landed at the food's own length is cut to the share, and the dose that landed pays a trickle in proportion to the protection (a cooked meal also pays by its quality, see {@link #onEaten}).
     */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        var food = event.getItem().get(DataComponents.FOOD);
        if (food == null || !(event.getEntity() instanceof ServerPlayer player))
            return;
        FoodState state = event.getItem().get(FDataComponents.FOOD_STATE.get());
        if (state != null && state.cook() > 0f)
            io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.ate(player,
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItem().getItem()).toString(),
                    Math.min(1f, state.cook()) * (1f - state.charred()) * (1f - state.rot()));
        float share = io.github.soundgoodizerfan.feedback.combat.skill.Skills.foodHazardShare(player);
        for (var possible : food.effects()) {
            MobEffectInstance made = possible.effect();
            var live = player.getEffect(made.getEffect());
            if (live == null || !isHazard(made) || live.getDuration() != made.getDuration())
                continue;
            if (share < 1f) {
                player.removeEffect(made.getEffect());
                player.addEffect(new MobEffectInstance(made.getEffect(), Math.max(1, Math.round(made.getDuration() * share)), made.getAmplifier()));
            }
            io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.hazardHandled(player, "food", 1f - share);
        }
    }

    private static boolean isHazard(MobEffectInstance e) {
        return e.getEffect().equals(MobEffects.POISON) || e.getEffect().equals(MobEffects.HUNGER) || e.getEffect().equals(MobEffects.CONFUSION);
    }

    private static void bite(ServerPlayer player, ItemStack stack) {
        var level = player.level();
        if (player.getCooldowns().isOnCooldown(stack.getItem()))
            return;
        player.getCooldowns().addCooldown(stack.getItem(), ProcessTuning.BITE_COOLDOWN_TICKS);
        if (stack.has(FDataComponents.CANNED.get()))
            Canning.settle(stack, level);
        boolean spoiled = false;
        if (stack.has(FDataComponents.FOOD_STATE.get()))
            spoiled = Cooking.findPerishable(level, stack).map(entry -> {
                FoodState now = Cooking.current(stack, entry, level, AmbientTable.at(level).value());
                stack.set(FDataComponents.FOOD_STATE.get(), now);
                Cooking.apply(stack, entry, now);
                return Cooking.spoilage(now) == 2;
            }).orElse(false);
        var effects = stack.get(DataComponents.FOOD).effects();
        String hazard = spoiled ? "spoiled" : "fine";
        for (var possible : effects) {
            MobEffectInstance effect = possible.effect();
            if (hazard.equals("fine") && effect.getEffect().equals(MobEffects.POISON))
                hazard = "poison";
            else if (hazard.equals("fine") && effect.getEffect().equals(MobEffects.HUNGER))
                hazard = "live";
            else if (hazard.equals("fine") && effect.getEffect().equals(MobEffects.CONFUSION))
                hazard = "burnt";
            // The class above is read from the food as it is; hardiness only softens what lands.
            float share = ProcessTuning.BITE_SHARE * io.github.soundgoodizerfan.feedback.combat.skill.Skills.foodHazardShare(player);
            if (level.random.nextFloat() < possible.probability() * share)
                player.addEffect(new MobEffectInstance(effect.getEffect(), Math.max(1, Math.round(effect.getDuration() * share)), effect.getAmplifier()));
        }
        if (hazard.equals("spoiled") || hazard.equals("burnt"))
            io.github.soundgoodizerfan.feedback.core.Awards.give(player, "progress/joke/burnt_offering");
        else if (hazard.equals("fine") && stack.get(FDataComponents.FOOD_STATE.get()) instanceof FoodState kept && kept.envTu() < 8f)
            // Kept cold and still sound: an Ice Box or Cold Room did the work.
            io.github.soundgoodizerfan.feedback.core.Awards.give(player, "progress/survival/winter_store");
        Component said = Component.translatable("message.feedback.bite." + hazard);
        // A potion laced in shows only as an aftertaste, and only when it stands out from the food (potions spec 7).
        var lace = stack.get(FDataComponents.LACING.get());
        var meal = stack.get(FDataComponents.MEAL.get());
        if (lace != null && meal != null && lace.detectable(meal, player))
            said = said.copy().append(" ").append(lace.nearestClass().map(c ->
                    Component.translatable("message.feedback.bite.aftertaste", Component.translatable("compound.feedback." + c))).orElse(Component.empty()));
        HudReadoutPayload.send(player, said, ProcessTuning.BITE_HUD_TICKS);
    }
}
