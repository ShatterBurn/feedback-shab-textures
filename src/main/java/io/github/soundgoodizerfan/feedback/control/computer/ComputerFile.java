// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import org.jetbrains.annotations.Nullable;

/**
 * One file on a Computer's internal disk or a removable medium: a kind (a program graph, an app id, a note), a name and its payload.
 * Everything a Computer keeps is a file; capacity counts files, and a hard byte cap per file ({@link ControlTuning#FILE_MAX_BYTES}) is
 * checked when one is written to a medium, because removable media carry their files in an item component and an oversized
 * component would book-ban the player carrying it.
 */
public record ComputerFile(Kind kind, String name, CompoundTag data) {

    public enum Kind {
        /** What the Programmer makes and a Controller runs: a {@code ProgramGraph}. */
        PROGRAM,
        /** An app the desktop runs: the id of a {@code ComputerApps} app. */
        APP,
        /** Text. */
        NOTE
    }

    private static final String KIND = "K", NAME = "N", DATA = "D";

    public ComputerFile {
        if (name.length() > ControlTuning.FILE_NAME_MAX)
            name = name.substring(0, ControlTuning.FILE_NAME_MAX);
    }

    /** How much space the file takes: the size of its saved form. */
    public int bytes() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            NbtIo.writeCompressed(save(), out);
            return out.size();
        } catch (java.io.IOException e) {
            return Integer.MAX_VALUE;
        }
    }

    /** Whether the file is small enough to be written to a medium at all. */
    public boolean fits() {
        return bytes() <= ControlTuning.FILE_MAX_BYTES;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putByte(KIND, (byte) kind.ordinal());
        tag.putString(NAME, name);
        tag.put(DATA, data.copy());
        return tag;
    }

    @Nullable
    public static ComputerFile load(CompoundTag tag, HolderLookup.Provider registries) {
        int k = tag.getByte(KIND);
        if (k < 0 || k >= Kind.values().length)
            return null;
        return new ComputerFile(Kind.values()[k], tag.getString(NAME), tag.getCompound(DATA).copy());
    }
}
