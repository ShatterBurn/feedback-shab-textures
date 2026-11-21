// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import net.minecraft.tags.FluidTags;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.registry.FTags;

/**
 * A fire. Something that holds a flame temperature, which is the ceiling on anything it heats.
 *
 * <h2>The source decides where its heat goes</h2>
 * A source has a {@link #heatFacing() facing}. A heat-accepting block touching that face gets the
 * source's whole temperature; one touching a side next to it gets {@link #sideShare()} of the way
 * there from ambient (heat lost on the way round), and one behind it gets {@link #backShare()}
 * (none). Fire sources always face up and keep the old fire-under-a-vessel layout; a coil, an arc
 * head or a creative source can be rotated. A source whose heat is very concentrated overrides
 * the shares. The receiver only sums what the sources around it give; it holds no opinion on
 * geometry, which is why {@link #at} is the one reader.
 *
 * <h2>Why a source is not a {@link ThermalBody}</h2>
 * A body has a mass and a temperature that other things change. A fire has a temperature and no
 * mass worth modelling -- nothing the player does cools a fire, they either feed it or they do
 * not. Keeping them apart is what stops the crucible needing to know whether it is over charcoal,
 * over lava, or over something a later slice invents: it asks the block below how hot it burns and
 * has no further opinion.
 */
public interface HeatSource {

    /** How hot this burns right now. Ambient when it is not lit. */
    Tu getFireTu();

    /**
     * What this did across {@code gap} ticks that nobody was there to tick, and have it spend the
     * fuel it burned. Called by whatever sits on top of it as it comes back from an unloaded
     * chunk -- see {@link Catchup}. Nothing by default: a source with no fuel (an element, a
     * flame that is only ever there) has nothing to have run out of.
     */
    default FireSpan catchUp(long gap) {
        return FireSpan.NONE;
    }

    /** The face this source radiates from. Fire sources are always up. */
    default Direction heatFacing() {
        return Direction.UP;
    }

    /** Reach on the four sides next to the facing. A concentrated source (arc, plasma) returns 1. */
    default float sideShare() {
        return ThermalTuning.HEAT_SIDE_SHARE;
    }

    /** Reach on the side behind the facing. Nothing by default. */
    default float backShare() {
        return 0f;
    }

    /**
     * Whether this source heats what stands at {@code target}. True by default; an induction source says no unless the receiver is
     * the kind of thing a field can heat (a metal charge, magnetic cookware). {@link #at} and {@link #spanAt} skip a source that says no.
     */
    default boolean heats(Level level, BlockPos target) {
        return true;
    }

    /** The facing a rotatable source block holds, up when its block has none. */
    static Direction facingOf(BlockState state) {
        return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.UP;
    }

    /** How much of this source's heat reaches the block {@code toward} it (the direction from the source to that block). */
    default float shareToward(Direction toward) {
        Direction face = heatFacing();
        if (toward == face)
            return 1f;
        if (toward == face.getOpposite())
            return backShare();
        return sideShare();
    }


    // --- who reaches a block --------------------------------------------------------------------

    /** The six face neighbours, then the twelve edge neighbours (two steps along two axes): where a source that reaches a block can stand. */
    int REACH = 18;

    Direction[] DIRS = Direction.values();

    /** The voxel of a finespace cell that {@code receiver} is hosted in, or -1 for a machine that is a whole block. */
    private static int voxelOf(Level level, BlockEntity receiver) {
        return level.getBlockEntity(receiver.getBlockPos()) instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell
                ? cell.voxelOf(receiver) : -1;
    }

    /**
     * The source that could reach {@code pos} (or voxel {@code voxel} of the cell there, -1 for the whole block) from candidate {@code i}
     * (0-5: the spot on that face; 6-17: a spot touching an edge), or null. A face neighbour always reaches; an edge neighbour reaches
     * only when it points at a spot next to the receiver, which is to say it faces one of the two faces it touches the receiver across
     * (the spot where it points, and the four round that, are lit).
     */
    private static HeatSource reaching(Level level, BlockPos pos, int voxel, int i) {
        Direction da, db = null;
        if (i < 6)
            da = DIRS[i];
        else {
            int n = i - 6, pair = n / 4, signs = n % 4;
            // pair 0: x,y   pair 1: x,z   pair 2: y,z; signs pick the direction on each axis
            Direction.Axis first = pair < 2 ? Direction.Axis.X : Direction.Axis.Y, second = pair == 0 ? Direction.Axis.Y : Direction.Axis.Z;
            da = Direction.fromAxisAndDirection(first, (signs & 1) == 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            db = Direction.fromAxisAndDirection(second, (signs & 2) == 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        }
        HeatSource source = voxel < 0 ? sourceBeside(level, pos, da, db) : sourceNextTo(level, pos, voxel, da, db);
        if (source == null || db == null)
            return source;
        Direction face = source.heatFacing();
        return face == da.getOpposite() || face == db.getOpposite() ? source : null;
    }

    /**
     * The source a whole block {@code pos} has across {@code da} (and {@code db}, if not null): the block entity itself, or, in a finespace
     * cell, the hottest source hosted on the voxels that touch the block.
     */
    private static HeatSource sourceBeside(Level level, BlockPos pos, Direction da, Direction db) {
        BlockPos at = db == null ? pos.relative(da) : pos.relative(da).relative(db);
        BlockEntity be = level.getBlockEntity(at);
        if (be instanceof HeatSource source)
            return source;
        if (!(be instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell))
            return null;
        HeatSource best = null;
        for (int v = 0; v < io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity.VOXELS; v++)
            if (touches(v, da) && (db == null || touches(v, db))) {
                HeatSource source = cell.hostedHeatSource(v);
                if (source != null && (best == null || source.getFireTu().value() > best.getFireTu().value()))
                    best = source;
            }
        return best;
    }

    /** Whether voxel {@code v} of a cell is on the face of the cell that touches a block lying toward {@code d} from the cell's other side. */
    private static boolean touches(int v, Direction d) {
        int c = d.getAxis() == Direction.Axis.X ? v & 3 : d.getAxis() == Direction.Axis.Y ? v >> 2 & 3 : v >> 4;
        return c == (d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity.N - 1);
    }

    /**
     * The source beside voxel {@code voxel} of the cell at {@code pos} across {@code da} (and {@code db}): a voxel of this cell, the matching
     * voxel of the next cell, or a whole block there.
     */
    private static HeatSource sourceNextTo(Level level, BlockPos pos, int voxel, Direction da, Direction db) {
        int n = io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity.N;
        int x = (voxel & 3) + da.getStepX(), y = (voxel >> 2 & 3) + da.getStepY(), z = (voxel >> 4) + da.getStepZ();
        if (db != null) {
            x += db.getStepX();
            y += db.getStepY();
            z += db.getStepZ();
        }
        BlockPos at = pos.offset(Math.floorDiv(x, n), Math.floorDiv(y, n), Math.floorDiv(z, n));
        BlockEntity be = level.getBlockEntity(at);
        if (be instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell)
            return cell.hostedHeatSource(io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity.index(Math.floorMod(x, n), Math.floorMod(y, n), Math.floorMod(z, n)));
        return be instanceof HeatSource source ? source : null;
    }

    /** The share {@code source}, found as candidate {@code i} of {@link #reaching}, gives: the face neighbours by facing, an edge neighbour its side share. */
    private static float shareAt(HeatSource source, int i) {
        return i < 6 ? source.shareToward(DIRS[i].getOpposite()) : source.sideShare();
    }


    // --- what a source pushes -------------------------------------------------------------------

    /**
     * Called from a source's own server tick: pushes its heat into the space it reaches, the same blocks {@link #at} reads from the
     * other end (the block it points at, the four round that, the four beside the source, and the back if it has a share). Every
     * {@link ThermalTuning#HEAT_PUSH_INTERVAL} ticks, and only when hot:
     * <ul>
     *   <li>a block with an item handler warms the stacks inside toward that spot's heat ({@link FTuning#HEAT_PUSH_ITEM_GRIP} of the
     *       gap a tick), unless it is a {@link ThermalBody} (a vessel heats its own contents through {@link #at}) or in the
     *       {@link FTags#NO_HEAT_PUSH} block tag, which is how a block opts out (anything else that reads {@link #at} itself);</li>
     *   <li>a dropped item there warms the same way;</li>
     *   <li>once a second, a living thing standing there is burned as a piece at that heat would burn it ({@link HotContact#burn}),
     *       so the band, and with it the damage, follows how hot the spot is.</li>
     * </ul>
     */
    static <T extends BlockEntity & HeatSource> void radiate(T self) {
        Level level = self.getLevel();
        if (level == null || level.isClientSide)
            return;
        long beat = Math.floorMod(level.getGameTime() + self.getBlockPos().asLong(), (long) ThermalTuning.HEAT_PUSH_INTERVAL);
        if (beat != 0L)
            return;
        float fire = self.getFireTu().value();
        if (fire <= ThermalTuning.WARM_TU.value())
            return;
        boolean scorch = (level.getGameTime() / ThermalTuning.HEAT_PUSH_INTERVAL) % ThermalTuning.HEAT_SCORCH_EVERY == 0;
        float ambient = AmbientTable.at(level).value();
        Direction face = self.heatFacing();
        BlockPos at = self.getBlockPos(), front = at.relative(face);
        reach(level, self, front, 1f, ambient, fire, scorch);
        float side = self.sideShare();
        if (side > 0f)
            for (Direction d : DIRS)
                if (d.getAxis() != face.getAxis()) {
                    reach(level, self, front.relative(d), side, ambient, fire, scorch);
                    reach(level, self, at.relative(d), side, ambient, fire, scorch);
                }
        if (self.backShare() > 0f)
            reach(level, self, at.relative(face.getOpposite()), self.backShare(), ambient, fire, scorch);
    }

    private static void reach(Level level, HeatSource source, BlockPos target, float share, float ambient, float fire, boolean scorch) {
        float tu = ambient + share * (fire - ambient);
        if (tu <= ThermalTuning.WARM_TU.value() || !source.heats(level, target))
            return;
        BlockEntity be = level.getBlockEntity(target);
        if (be != null && !(be instanceof ThermalBody) && !be.getBlockState().is(FTags.NO_HEAT_PUSH)) {
            IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, target, null);
            if (items != null) {
                boolean changed = false;
                for (int slot = 0; slot < items.getSlots(); slot++)
                    changed |= warm(items.getStackInSlot(slot), tu, level);
                if (changed)
                    be.setChanged();
            }
        }
        AABB space = new AABB(target);
        for (ItemEntity dropped : level.getEntitiesOfClass(ItemEntity.class, space))
            warm(dropped.getItem(), tu, level);
        if (scorch && tu >= ThermalTuning.CONTACT_PAIN_TU.value())
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, space))
                HotContact.burn(victim, new Tu(tu));
    }

    private static boolean warm(ItemStack stack, float tu, Level level) {
        if (stack.isEmpty())
            return false;
        float now = ItemHeat.get(stack, level).value();
        if (tu <= now)
            return false;
        ItemHeat.set(stack, new Tu(now + ThermalTuning.HEAT_PUSH_ITEM_GAIN * (tu - now)), level);
        return true;
    }

    /** {@link #at} across a gap: the fire the sources round {@code pos} gave over {@code gap} ticks. */
    static FireSpan spanAt(Level level, BlockPos pos, long gap) {
        return spanAt(level, pos, -1, gap);
    }

    /** {@link #spanAt(Level, BlockPos, long)} for a machine that may be hosted in a finespace cell: its voxel's neighbours, not the cell's. */
    static FireSpan spanAt(Level level, BlockEntity receiver, long gap) {
        return spanAt(level, receiver.getBlockPos(), voxelOf(level, receiver), gap);
    }

    private static FireSpan spanAt(Level level, BlockPos pos, int voxel, long gap) {
        FireSpan best = FireSpan.NONE;
        Tu ambient = null;
        for (int i = 0; i < REACH; i++) {
            HeatSource source = reaching(level, pos, voxel, i);
            if (source == null)
                continue;
            float share = shareAt(source, i);
            if (share <= 0f || !source.heats(level, pos))
                continue;
            FireSpan span = source.catchUp(gap).clampedTo(gap);
            if (span.litTicks() <= 0L)
                continue;
            if (ambient == null)
                ambient = AmbientTable.at(level);
            float tu = ambient.value() + share * (span.tu() - ambient.value());
            if (tu > best.tu())
                best = new FireSpan(span.litTicks(), tu);
        }
        if (onFloor(voxel) && level.getFluidState(pos.below()).is(FluidTags.LAVA) && ThermalTuning.FIRE_TU_LAVA.value() > best.tu()) {
            io.github.soundgoodizerfan.feedback.core.Awards.near(level, pos, "progress/heat/lava_fire");
            return new FireSpan(gap, ThermalTuning.FIRE_TU_LAVA.value());
        }
        return best;
    }

    /**
     * How cold the cold reaching {@code pos} is: the coldest of the sources round it that hold a temperature below ambient, each
     * at the share its own facing gives this side. Ambient when none does. {@link #at} ignores those sources; a vessel that can be
     * chilled (the reactor) reads this when no fire reaches it.
     */
    static Tu coldAt(Level level, BlockPos pos) {
        return coldAt(level, pos, -1);
    }

    /** {@link #coldAt(Level, BlockPos)} for a machine that may be hosted in a finespace cell. */
    static Tu coldAt(Level level, BlockEntity receiver) {
        return coldAt(level, receiver.getBlockPos(), voxelOf(level, receiver));
    }

    private static Tu coldAt(Level level, BlockPos pos, int voxel) {
        Tu ambient = AmbientTable.at(level);
        float best = ambient.value();
        for (int i = 0; i < REACH; i++) {
            HeatSource source = reaching(level, pos, voxel, i);
            if (source == null)
                continue;
            float share = shareAt(source, i);
            float fire = source.getFireTu().value();
            if (share <= 0f || fire >= ambient.value() || !source.heats(level, pos))
                continue;
            best = Math.min(best, ambient.value() + share * (fire - ambient.value()));
        }
        return new Tu(best);
    }

    /**
     * How hot the heat reaching {@code pos} is: the hottest of the sources round it, each at the
     * share its own facing gives this side.
     *
     * <h3>Lava is handled here and nowhere else</h3>
     * It is a plain vanilla block with no block entity, so it cannot push. The block under a
     * position being lava is the one special case, as it always was, and by tag so any mod's lava
     * counts.
     */
    static Tu at(Level level, BlockPos pos) {
        return at(level, pos, -1);
    }

    /**
     * {@link #at(Level, BlockPos)} for a machine that may be hosted in a finespace cell: a hosted machine reads the sources in the voxels
     * round its own (in its cell, the next cell or the whole block beside it), not round the cell.
     */
    static Tu at(Level level, BlockEntity receiver) {
        return at(level, receiver.getBlockPos(), voxelOf(level, receiver));
    }

    /** Whether a receiver ({@code voxel}, -1 for a whole block) stands on the floor of its block, so what is under the block is under it. */
    private static boolean onFloor(int voxel) {
        return voxel < 0 || (voxel >> 2 & 3) == 0;
    }

    private static Tu at(Level level, BlockPos pos, int voxel) {
        Tu ambient = AmbientTable.at(level);
        float best = ambient.value();
        for (int i = 0; i < REACH; i++) {
            HeatSource source = reaching(level, pos, voxel, i);
            if (source == null)
                continue;
            float share = shareAt(source, i);
            if (share <= 0f || !source.heats(level, pos))
                continue;
            float fire = source.getFireTu().value();
            if (fire <= ambient.value())
                continue;
            best = Math.max(best, ambient.value() + share * (fire - ambient.value()));
        }
        if (onFloor(voxel) && level.getFluidState(pos.below()).is(FluidTags.LAVA))
            best = Math.max(best, ThermalTuning.FIRE_TU_LAVA.value());
        return new Tu(best);
    }
}
