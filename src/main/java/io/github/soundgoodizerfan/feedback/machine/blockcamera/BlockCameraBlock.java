// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.blockcamera;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FacingShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Block Camera: a traffic camera on a steel pole, found standing in the world (worldgen/BlockCameraFeature), never made. A
 * player carrying Bad Omen who comes within {@link BlockCameraWatch#RANGE} blocks is locked on to: the head turns to face them
 * and a red light blinks for {@link BlockCameraWatch#LOCK_TICKS} ticks. If they still carry the omen then, the camera reports
 * them the way a village would: their Bad Omen becomes a Raid Omen and the raid comes to the camera, once. After that the camera
 * is {@link BlockCameraStage#SPENT}: a dead lens that never fires again. Broken, it gives up its parts (the loot table).
 * <p>
 * The camera never ticks. Players with Bad Omen find it ({@link BlockCameraWatch}), and a lock-on runs on scheduled block ticks.
 */
public class BlockCameraBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BlockCameraStage> STAGE = EnumProperty.create("stage", BlockCameraStage.class);

    /** Mirrors models/block/block_camera.json drawn facing north: the pole top, the camera hung off its side, the tilted panel above. */
    private static final VoxelShape[] SHAPES = FacingShapes.horizontal(new int[][]{
            {6, 0, 6, 10, 12, 10}, {10, 3, 3, 15, 10, 11}, {1, 12, 2, 15, 16, 14}});

    public BlockCameraBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(STAGE, BlockCameraStage.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockCameraBlockEntity(pos, state);
    }

    /** Starts a lock-on: turns the head to the player, lights the lamp, clicks the shutter and schedules the first blink. */
    void lockOn(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, BlockCameraBlockEntity camera) {
        double dx = player.getX() - (pos.getX() + 0.5), dz = player.getZ() - (pos.getZ() + 0.5);
        camera.lock(player.getUUID());
        level.setBlock(pos, state.setValue(FACING, Direction.getNearest(dx, 0, dz)).setValue(STAGE, BlockCameraStage.LOCK_ON), 3);
        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 1.0f, 1.7f);
        level.scheduleTick(pos, this, BlockCameraWatch.BLINK_TICKS);
    }

    /** One blink of a lock-on. Loses the player (gone, no omen, out of reach) and the camera stands down; keeps them and, on the last blink, reports. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(STAGE).locked() || !(level.getBlockEntity(pos) instanceof BlockCameraBlockEntity camera))
            return;
        ServerPlayer player = camera.target(level);
        if (player == null || !BlockCameraWatch.reportable(level, pos, player, BlockCameraWatch.LOSE_RANGE)) {
            camera.release();
            level.setBlock(pos, state.setValue(STAGE, BlockCameraStage.IDLE), 3);
            return;
        }
        if (camera.blink() < BlockCameraWatch.BLINKS) {
            BlockCameraStage next = state.getValue(STAGE) == BlockCameraStage.LOCK_ON ? BlockCameraStage.LOCK_OFF : BlockCameraStage.LOCK_ON;
            level.setBlock(pos, state.setValue(STAGE, next), 3);
            if (next == BlockCameraStage.LOCK_ON)
                level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.5f, 1.7f);
            level.scheduleTick(pos, this, BlockCameraWatch.BLINK_TICKS);
            return;
        }
        report(level, pos, state, player, camera);
    }

    /**
     * What a village does to a player with Bad Omen, done at the camera: the omen turns into a Raid Omen of the same strength and the raid
     * is raised at this spot when it runs out (vanilla's own path, so a village within reach of the spot is the raid's centre).
     */
    private static void report(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, BlockCameraBlockEntity camera) {
        MobEffectInstance omen = player.getEffect(MobEffects.BAD_OMEN);
        camera.release();
        if (omen == null) {
            level.setBlock(pos, state.setValue(STAGE, BlockCameraStage.IDLE), 3);
            return;
        }
        player.removeEffect(MobEffects.BAD_OMEN);
        player.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 600, omen.getAmplifier()));
        player.setRaidOmenPosition(pos);
        level.setBlock(pos, state.setValue(STAGE, BlockCameraStage.SPENT), 3);
        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 1.0f, 0.8f);
    }
}
