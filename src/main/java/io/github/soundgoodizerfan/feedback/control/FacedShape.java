// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One hitbox per horizontal facing from the boxes of a model drawn with its front on north: {@code {x1, y1, z1, x2, y2, z2}} in sixteenths,
 * mirroring the model's elements (keep in step with the generator script). A blockstate {@code y} rotation is clockwise seen from above, so
 * north to east maps a point (x, z) to (16 - z, x); a box rotates by mapping its two corners and sorting. Fill openings the player cannot
 * reach into and skip parts finer than a tenth of a block.
 */
public final class FacedShape {

    private FacedShape() {
    }

    public static Map<Direction, VoxelShape> of(int[][] boxes) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (int[] b : boxes) {
                int x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
                int[] r = switch (d) {
                    case EAST -> new int[]{16 - z2, x1, 16 - z1, x2};
                    case SOUTH -> new int[]{16 - x2, 16 - z2, 16 - x1, 16 - z1};
                    case WEST -> new int[]{z1, 16 - x2, z2, 16 - x1};
                    default -> new int[]{x1, z1, x2, z2};
                };
                shape = Shapes.or(shape, Block.box(r[0], b[1], r[1], r[2], b[4], r[3]));
            }
            shapes.put(d, shape);
        }
        return shapes;
    }
}
