/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.ArrayList;
import java.util.List;

/**
 * Texel size for micro voxels ({@code docs/dev/vehicle.md}): a voxel is drawn at a quarter of its
 * block's size, and its texture must not shrink with it -- a texel stays 1/16 of a block, so a voxel face
 * shows 4 by 4 texels. The cell is treated as one block: each quad's texture coordinates are read as if
 * the quad sat at its voxel's place in the cell, so a cell full of one block shows that block's real
 * texture across it, seams and all, and a part of a block (a shaft, a stub) takes the same patch of
 * texture as it would on the real block.
 */
public final class MicroTexels {

    private MicroTexels() {
    }

    /**
     * {@code quads}, drawn in the voxel at ({@code vx}, {@code vy}, {@code vz}) of a cell {@code n} voxels
     * across, with their texture coordinates moved to that voxel's patch of the sprite.
     */
    public static List<BakedQuad> fit(List<BakedQuad> quads, int vx, int vy, int vz, int n) {
        if (quads.isEmpty())
            return quads;
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad q : quads)
            out.add(fit(q, vx, vy, vz, n));
        return out;
    }

    /**
     * The quad's texture is an affine function of position across its plane; the voxel sits at
     * {@code (voxel + p) / n} in the cell, where the same function gives the corner's new coordinates.
     */
    public static BakedQuad fit(BakedQuad quad, int vx, int vy, int vz, int n) {
        int[] v = quad.getVertices().clone();
        float[][] p = new float[4][3];
        float[][] t = new float[4][2];
        for (int k = 0; k < 4; k++) {
            for (int a = 0; a < 3; a++)
                p[k][a] = Float.intBitsToFloat(v[k * 8 + a]);
            t[k][0] = Float.intBitsToFloat(v[k * 8 + 4]);
            t[k][1] = Float.intBitsToFloat(v[k * 8 + 5]);
        }
        // The plane's two edges from corner 0 and what the texture does along each.
        float[] e1 = sub(p[1], p[0]), e2 = sub(p[3], p[0]);
        float g11 = dot(e1, e1), g12 = dot(e1, e2), g22 = dot(e2, e2);
        float det = g11 * g22 - g12 * g12;
        if (det < 1e-10f)
            return quad;
        float[] voxel = {vx, vy, vz};
        TextureAtlasSprite sprite = quad.getSprite();
        for (int k = 0; k < 4; k++) {
            // Where the corner moves: from its place in the voxel's block to its place in the cell.
            float[] d = new float[3];
            for (int a = 0; a < 3; a++)
                d[a] = (voxel[a] + p[k][a]) / n - p[k][a];
            float r1 = dot(e1, d), r2 = dot(e2, d);
            float a1 = (g22 * r1 - g12 * r2) / det, a2 = (g11 * r2 - g12 * r1) / det;
            for (int c = 0; c < 2; c++) {
                float moved = t[k][c] + a1 * (t[1][c] - t[0][c]) + a2 * (t[3][c] - t[0][c]);
                float lo = c == 0 ? sprite.getU0() : sprite.getV0(), hi = c == 0 ? sprite.getU1() : sprite.getV1();
                v[k * 8 + 4 + c] = Float.floatToRawIntBits(Math.max(Math.min(lo, hi), Math.min(Math.max(lo, hi), moved)));
            }
        }
        return new BakedQuad(v, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    private static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}
