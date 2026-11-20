// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.note;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The note block, built from parts ({@code modular_type/note_block}): a body cut to a pitch and one or two noisemakers, each an instrument made of
 * its own material, which sound together when it is struck or signalled. It makes no sound of its own: a block with no noisemaker is silent.
 * Pitch is the note, 0 to 24, set by the body and stepped by hand (an empty hand steps it and plays it); a rising signal or a punch plays it.
 * Vanilla takes the instrument from the block below; here it is the part. Vanilla's block is swapped for it, keeping its pitch and instrument.
 */
public class FeedbackNoteBlock extends Block implements EntityBlock {

    public static final IntegerProperty NOTE = BlockStateProperties.NOTE;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public FeedbackNoteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NOTE, 0).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NOTE, POWERED);
    }

    /** A block placed from its item starts on the pitch its body was cut to. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof NoteBlockEntity note)
            level.setBlock(pos, state.setValue(NOTE, Math.max(0, Math.min(24, note.pitch()))), Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        BlockState next = state.cycle(NOTE);
        level.setBlock(pos, next, Block.UPDATE_ALL);
        play(level, pos, next);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide)
            play(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            if (powered && !level.isClientSide)
                play(level, pos, state);
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    /** One note from every fitted noisemaker: nothing at all if none is fitted. */
    public static void play(Level level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos.above()).isAir() || !(level.getBlockEntity(pos) instanceof NoteBlockEntity note))
            return;
        int n = state.getValue(NOTE);
        boolean sounded = false;
        for (int i = 0; i < NoteTuning.VOICES; i++) {
            NoteBlockInstrument instrument = note.instrument(i);
            if (instrument == null)
                continue;
            float pitch = (float) Math.pow(2.0, (n - 12) / 12.0 + note.shift(i));
            level.playSeededSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, instrument.getSoundEvent(), SoundSource.RECORDS,
                    note.volume(i), pitch, level.random.nextLong());
            sounded = true;
        }
        if (sounded && level instanceof net.minecraft.server.level.ServerLevel server)
            server.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0, n / 24.0, 0, 0, 1);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NoteBlockEntity(pos, state);
    }
}
