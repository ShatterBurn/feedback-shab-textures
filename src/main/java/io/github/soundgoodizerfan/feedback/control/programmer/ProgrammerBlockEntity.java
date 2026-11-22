// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import io.github.soundgoodizerfan.feedback.registry.FTags;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataLinkManager;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

/**
 * Where a Punch Card's {@link ProgramGraph} is authored. Holds the card being worked on plus a
 * separate {@link #working} graph -- printing is the one moment they become the same thing.
 * <p>
 * A printed card (its own {@code PROGRAM_GRAPH} component non-empty) refuses every further
 * commit: spec §4's hard-rewrite tier is enforced here, not by the item. Edits and prints are
 * checked against {@link ControllerLimits#MAX}, a Controller with every upgrade fitted: the
 * Programmer cannot know which Controller the card will end up in, so the Controller refuses a
 * card its own fitted upgrades cannot run.
 */
public class ProgrammerBlockEntity extends BlockEntity implements Container, MenuProvider {

    private static final String CARD = "Card";
    private static final String WORKING = "Working";
    private static final String POSITIONS = "Positions";

    private ItemStack card = ItemStack.EMPTY;
    private ProgramGraph working = ProgramGraph.EMPTY;

    /**
     * Where each card sits on the {@code ProgrammerScreen} canvas -- cosmetic only, never part
     * of {@link ProgramGraph} (a printed card carries no layout, only wiring). Keyed by node id,
     * same lifetime as {@link #working}.
     */
    private final Map<Integer, NodePos> positions = new HashMap<>();

    public ProgrammerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PROGRAMMER.get(), pos, state);
    }

    /** Whether this Programmer is the electric kind, which takes the Memory Board and the Floppy Disk; the mechanical one takes the Punch Card alone. */
    protected boolean electric() {
        return getBlockState().getBlock() instanceof ProgrammerBlock block && block.electric();
    }

    /** Whether the medium may sit in this Programmer's slot. */
    public boolean takes(ItemStack stack) {
        return stack.is(electric() ? FTags.ELECTRIC_CARDS : FTags.MECHANICAL_CARDS)
                && !io.github.soundgoodizerfan.feedback.control.computer.ComputerApps.isApp(stack);
    }

    /** The Programmer at {@code pos}: the block entity itself, or the one a Computer carries. */
    @Nullable
    public static ProgrammerBlockEntity at(net.minecraft.world.level.Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ProgrammerBlockEntity programmer)
            return programmer;
        return be instanceof ProgrammerProvider provider ? provider.programmer() : null;
    }

    public void saveEmbedded(CompoundTag tag, HolderLookup.Provider registries) {
        saveAdditional(tag, registries);
    }

    public void loadEmbedded(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    /** Forgets the working copy and its layout: the next medium opened here starts from what it holds. */
    public void resetWorking() {
        working = ProgramGraph.EMPTY;
        positions.clear();
    }

    public boolean isPrinted() {
        return !card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY).nodes().isEmpty();
    }

    public Map<Integer, NodePos> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    /** What a screen should draw: the printed graph if there is one, else the working copy. */
    public ProgramGraph getDisplayedGraph() {
        ProgramGraph printed = card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        return printed.nodes().isEmpty() ? working : printed;
    }

    /**
     * Replaces the working copy with what a {@code ProgrammerScreen} edited locally, then prints
     * if asked. The client is not trusted: {@link ProgramGraph#findEditError} refuses a graph whose
     * wiring could never have been drawn. A card names a signal, a kind and a color, never a block, so
     * a made-up one only ever matches nothing. Last commit wins if two players edit the same Programmer at once.
     */
    public void commit(ProgramGraph graph, Map<Integer, NodePos> newPositions, boolean print, Player player) {
        if (level == null || level.isClientSide || !stillValid(player))
            return;
        if (isPrinted()) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("feedback.programmer.already_printed"));
            return;
        }
        Optional<String> error = graph.findEditError(ControllerLimits.MAX);
        if (error.isPresent()) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("feedback.programmer.error." + error.get()));
            return;
        }

        working = graph;
        positions.clear();
        newPositions.forEach((id, pos) -> {
            if (working.find(id).isPresent())
                positions.put(id, pos);
        });
        if (print)
            print(player);
        sync();
    }

    /**
     * A soft medium (Floppy Disk, USB Drive) wiped: its printed program comes back as the working copy, the card is blank again, and
     * the usual edit-then-print flow writes over it. A hard medium cannot be erased.
     */
    public void erase(Player player) {
        if (level == null || level.isClientSide || !stillValid(player) || card.isEmpty())
            return;
        if (!card.is(io.github.soundgoodizerfan.feedback.registry.FTags.SOFT_CARDS)) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("feedback.programmer.already_printed"));
            return;
        }
        ProgramGraph printed = card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        if (printed.nodes().isEmpty())
            return;
        working = printed;
        positions.clear();
        card.remove(FDataComponents.PROGRAM_GRAPH);
        card.remove(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("feedback.programmer.erased"));
    }

    private void print(Player player) {
        if (card.isEmpty()) {
            // card is ItemStack.EMPTY here -- the shared vanilla singleton. card.set(...) below
            // mutates whatever stack `card` refers to, so skipping this check would corrupt that
            // singleton's component map for every empty stack in the game, not just this one.
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("feedback.programmer.no_card"));
            return;
        }
        Optional<String> error = working.findError(ControllerLimits.ofMedium(card));
        if (error.isPresent()) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("feedback.programmer.error." + error.get()));
            return;
        }
        card.set(FDataComponents.PROGRAM_GRAPH, working);
        // Blank cards stack; a card holding a program is one of a kind (author, 2026-10-11).
        card.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1);
        io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(player, Component.translatable("feedback.programmer.printed"));
    }

    // --- Container (one Punch Card slot) ---------------------------------------------------

    /**
     * One card at a time, now blank cards stack: printing writes onto the stack in the slot, so a
     * stack of sixteen would come out as sixteen copies.
     */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return card.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return card;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = card.split(count);
        if (card.isEmpty())
            resetWorking();
        if (!result.isEmpty())
            sync(); // an open screen follows the block entity, so the reset has to reach it
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = card;
        card = ItemStack.EMPTY;
        resetWorking();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // No resetWorking: a program drafted before the card went in is kept for it.
        card = stack;
        card.limitSize(getMaxStackSize(card));
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return card.isEmpty() && takes(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        card = ItemStack.EMPTY;
        resetWorking();
    }

    // --- MenuProvider ---------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("feedback.container.programmer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ProgrammerMenu(id, playerInventory, this);
    }

    // --- client sync ------------------------------------------------------------------------

    /** Also called from {@link ProgrammerBlock} right before opening the menu -- the screen reads {@link #working}/{@link #positions} straight off this block entity, not through a synced menu slot, so the opening player needs a fresh push rather than relying on whatever sync happened to fire during the last edit. */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!card.isEmpty())
            tag.put(CARD, card.save(registries, new CompoundTag()));
        tag.put(WORKING, ProgramGraph.CODEC.encodeStart(NbtOps.INSTANCE, working).getOrThrow());

        ListTag positionList = new ListTag();
        positions.forEach((id, pos) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Id", id);
            entry.putInt("X", pos.x());
            entry.putInt("Y", pos.y());
            positionList.add(entry);
        });
        tag.put(POSITIONS, positionList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        card = tag.contains(CARD)
                ? ItemStack.parse(registries, tag.getCompound(CARD)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        working = tag.contains(WORKING)
                ? ProgramGraph.CODEC.parse(NbtOps.INSTANCE, tag.get(WORKING)).result().orElse(ProgramGraph.EMPTY)
                : ProgramGraph.EMPTY;

        positions.clear();
        for (Tag entry : tag.getList(POSITIONS, Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) entry;
            positions.put(c.getInt("Id"), new NodePos(c.getInt("X"), c.getInt("Y")));
        }
    }
}
