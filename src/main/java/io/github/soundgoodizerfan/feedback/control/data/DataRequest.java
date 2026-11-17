// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * What a program card asks of the data blocks bound to its Controller: a {@code signal} to read (null for an
 * actuator), a preferred {@code kind} (null for any), a link {@code color} (-1 for any) and, for a reading, how
 * several matches combine. It names no block and no position.
 * <p>
 * Design from Super Factory Manager (MPL-2.0, design only): a program names a request and a separate binding resolves it. Rejected there:
 * free-text labels, position maps, capability probing, a silent unordered union of matches. See {@code THIRD-PARTY-LICENSES.md}.
 */
public record DataRequest(@Nullable ResourceLocation signal, @Nullable ResourceLocation kind, int color, Reduction reduction) {

    public static final int ANY = -1;

    /** A node bound to a Controller, as the resolver sees it. */
    public record Candidate(DataNode node, DataNodeRef ref, int color, DataSpec spec) {
    }

    /**
     * The nodes this request means. A reading prefers nodes of its kind and falls back to any node offering the
     * signal when none of that kind is bound, so one program runs on a crucible or an arc hearth; an actuator
     * never falls back, since driving whatever else happens to be switchable would be a hazard.
     */
    public List<Candidate> match(Iterable<Candidate> bound) {
        List<Candidate> preferred = new ArrayList<>(), others = new ArrayList<>();
        for (Candidate c : bound) {
            if (color != ANY && c.color() != color)
                continue;
            boolean capable = signal == null ? c.spec().writes() : c.spec().reads(signal);
            if (!capable)
                continue;
            if (kind == null || c.spec().isOfKind(kind))
                preferred.add(c);
            else
                others.add(c);
        }
        return !preferred.isEmpty() || signal == null ? preferred : others;
    }
}
