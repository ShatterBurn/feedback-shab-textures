// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.thresher;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A threshing drum on a shaft: a grain plant goes in, and the drum beats the grain free of its stem and husk, so the seed, the straw and the chaff come out
 * as three things (operation {@link io.github.soundgoodizerfan.feedback.process.Operation#THRESH}). It knows no recipe: it threshes what is a grain with a seed
 * form, shreds whatever soft thing else goes in (spoiled, nothing out) and is jammed by anything harder than its teeth, which wear by that excess and never
 * by running. A drum spun faster than {@link MachineTuning#THRESHER_FLING_RPM} throws the seed out onto the ground instead of into its tray. Worn teeth thresh
 * slower, down to a floor, and never stop.
 * <p>
 * Ticks only while it holds something and is turning; nothing is sent to the client.
 */
public class ThresherBlockEntity extends RotationNode {

    /** Slot 0 is what goes in; 1 seed, 2 straw, 3 chaff come out. */
    private static final int IN = 0, SEED = 1, STRAW = 2, CHAFF = 3;

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private float progress;
    private float teeth = 1f;
    private boolean jammed;

    public ThresherBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.THRESHER.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return MachineTuning.THRESHER_LOAD_SU;
    }

    /** What a hopper sees: it may fill the hopper slot and empty the three trays, and nothing else. */
    public IItemHandler port() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return items.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot == IN && !jammed ? items.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == IN ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == IN && !jammed;
            }
        };
    }

    public boolean isJammed() {
        return jammed;
    }

    /** Right-click with a stack: it goes into the hopper unless jammed. Returns whether it was taken (all of it, up to the slot's room). */
    public boolean feed(ItemStack stack, boolean simulate) {
        if (jammed || stack.isEmpty())
            return false;
        ItemStack left = items.insertItem(IN, stack.copy(), simulate);
        if (left.getCount() == stack.getCount())
            return false;
        if (!simulate)
            stack.setCount(left.getCount());
        return true;
    }

    /** An empty hand: a jam is cleared and the jamming thing comes out; else the trays are emptied. */
    public ItemStack take() {
        if (jammed) {
            jammed = false;
            ItemStack stuck = items.extractItem(IN, items.getStackInSlot(IN).getCount(), false);
            setChanged();
            return stuck;
        }
        for (int slot = SEED; slot <= CHAFF; slot++) {
            ItemStack out = items.extractItem(slot, items.getStackInSlot(slot).getCount(), false);
            if (!out.isEmpty())
                return out;
        }
        return ItemStack.EMPTY;
    }

    public void dropContents() {
        for (int slot = 0; slot < items.getSlots(); slot++)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || jammed || items.getStackInSlot(IN).isEmpty())
            return;
        float rpm = Math.abs(getRpm().value());
        if (rpm < MachineTuning.THRESHER_MIN_RPM.value())
            return;
        progress += rpm / MachineTuning.THRESHER_REFERENCE_RPM.value() / MachineTuning.THRESHER_TICKS_PER_PLANT * teeth;
        if (progress < 1f)
            return;
        if (thresh(server, rpm))
            progress -= 1f;
        else
            progress = 1f;
    }

    /** One plant through the drum. False when a tray is full and the drum stalls with the plant still in. */
    private boolean thresh(ServerLevel server, float rpm) {
        ItemStack in = items.getStackInSlot(IN);
        var found = Material.find(ClientMaterials.on(server), in);
        Item seed = found.filter(l -> l.material().kind().filter(k -> k == Material.Kind.GRAIN).isPresent())
                .flatMap(l -> l.material().item(l.id(), "seed")).orElse(null);
        if (seed == null) {
            // Not a grain: soft things are shredded (spoiled), a hard one jams the drum and the excess wears the teeth.
            // How many times harder than the teeth the piece is (a ratio, not a unit); 0 for an unknown thing, which the drum treats as soft.
            float excess = found.map(l -> l.material().hardness().value() / MachineTuning.THRESHER_TEETH_HARDNESS.value() - 1f).orElse(0f);
            if (excess > 0f) {
                jammed = true;
                teeth = Math.max(MachineTuning.THRESHER_TEETH_FLOOR, teeth - excess * MachineTuning.THRESHER_JAM_WEAR);
                server.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4f, 0.6f);
            } else
                items.extractItem(IN, 1, false);
            setChanged();
            return true;
        }
        boolean fling = rpm > MachineTuning.THRESHER_FLING_RPM.value();
        ItemStack seeds = new ItemStack(seed, MachineTuning.THRESHER_SEEDS_PER_PLANT);
        // The husks are the grain's own: the forms its material names, none if it names none.
        ItemStack straw = found.flatMap(l -> l.material().item(l.id(), "straw")).map(ItemStack::new).orElse(ItemStack.EMPTY);
        ItemStack chaff = found.flatMap(l -> l.material().item(l.id(), "chaff")).map(ItemStack::new).orElse(ItemStack.EMPTY);
        if (!(fling || items.insertItem(SEED, seeds, true).isEmpty()) || !items.insertItem(STRAW, straw, true).isEmpty()
                || !items.insertItem(CHAFF, chaff, true).isEmpty())
            return false;
        items.extractItem(IN, 1, false);
        if (fling) {
            // Past the drum's speed the grain is thrown out of the tray and lands on the ground.
            ItemEntity flung = new ItemEntity(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, seeds);
            flung.setDeltaMovement((server.random.nextDouble() - 0.5) * 0.4, 0.25, (server.random.nextDouble() - 0.5) * 0.4);
            server.addFreshEntity(flung);
        } else
            items.insertItem(SEED, seeds, false);
        items.insertItem(STRAW, straw, false);
        items.insertItem(CHAFF, chaff, false);
        server.playSound(null, worldPosition, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.4f, 1f);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putFloat("Progress", progress);
        tag.putFloat("Teeth", teeth);
        tag.putBoolean("Jammed", jammed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = tag.getFloat("Progress");
        teeth = tag.contains("Teeth") ? tag.getFloat("Teeth") : 1f;
        jammed = tag.getBoolean("Jammed");
    }
}
