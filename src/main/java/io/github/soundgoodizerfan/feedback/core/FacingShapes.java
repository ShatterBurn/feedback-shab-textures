// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hitboxes for an element model drawn with its front on north and turned by the blockstate's {@code y} rotation (north 0, east 90,
 * south 180, west 270). The boxes are the model's, in sixteenths; the table is indexed by {@link Direction#get2DDataValue()}.
 */
public final class FacingShapes {

    private FacingShapes() {
    }

    /** The shape of {@code boxes} ({x1, y1, z1, x2, y2, z2} each, facing north) for each horizontal facing. */
    public static VoxelShape[] horizontal(int[][] boxes) {
        VoxelShape[] out = new VoxelShape[4];
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape s = Shapes.empty();
            for (int[] b : boxes) {
                double[] p = turn(d, b[0], b[2]), q = turn(d, b[3], b[5]);
                s = Shapes.or(s, Block.box(Math.min(p[0], q[0]), b[1], Math.min(p[1], q[1]),
                        Math.max(p[0], q[0]), b[4], Math.max(p[1], q[1])));
            }
            out[d.get2DDataValue()] = s;
        }
        return out;
    }

    /** A point (x, z) turned clockwise seen from above by the facing: north none, east (16 - z, x), south (16 - x, 16 - z), west (z, 16 - x). */
    private static double[] turn(Direction d, int x, int z) {
        return switch (d) {
            case EAST -> new double[]{16 - z, x};
            case SOUTH -> new double[]{16 - x, 16 - z};
            case WEST -> new double[]{z, 16 - x};
            default -> new double[]{x, z};
        };
    }
}
