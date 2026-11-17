// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * How a removable medium and a {@link ComputerFile} turn into each other. A medium shows as the one file it holds: a printed program
 * ({@code PROGRAM_GRAPH}) is a PROGRAM file, an app disk ({@code APP}) an APP file named for the app. Copying and moving between the
 * internal disk and a bay is a read of one and a write of the other, and a write that does not fit changes nothing.
 */
public final class ComputerFiles {

    private static final String GRAPH = "G", TRANSIENT = "ComputerTransient";

    /** Marks a stack made only to run or edit an internal file: it never leaves the Computer and is discarded rather than dropped. */
    public static ItemStack markTransient(ItemStack stack) {
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(TRANSIENT, true));
        return stack;
    }

    public static boolean isTransient(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).contains(TRANSIENT);
    }

    private ComputerFiles() {
    }

    public static ComputerFile program(String name, ProgramGraph graph) {
        CompoundTag data = new CompoundTag();
        ProgramGraph.CODEC.encodeStart(NbtOps.INSTANCE, graph).result().ifPresent(t -> data.put(GRAPH, t));
        return new ComputerFile(ComputerFile.Kind.PROGRAM, name, data);
    }

    public static ComputerFile app(String id) {
        return new ComputerFile(ComputerFile.Kind.APP, id, new CompoundTag());
    }

    /** The program a PROGRAM file holds, empty for any other kind or a file that does not decode. */
    public static ProgramGraph graphOf(ComputerFile file) {
        if (file.kind() != ComputerFile.Kind.PROGRAM || !file.data().contains(GRAPH))
            return ProgramGraph.EMPTY;
        Tag tag = file.data().get(GRAPH);
        return ProgramGraph.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(ProgramGraph.EMPTY);
    }

    /** The one file a medium holds, or null for a blank one. */
    @Nullable
    public static ComputerFile read(ItemStack medium) {
        if (medium.isEmpty())
            return null;
        String app = ComputerApps.idOf(medium);
        if (app != null)
            return app(app);
        ProgramGraph graph = medium.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        return graph.nodes().isEmpty() ? null : program(medium.getHoverName().getString(), graph);
    }

    /** Whether {@code medium} is a lone soft medium with nothing on it: a Floppy Disk or USB Drive a file may be written to. */
    public static boolean blank(ItemStack medium) {
        return medium.getCount() == 1 && medium.is(FTags.SOFT_CARDS) && read(medium) == null;
    }

    /** Writes {@code file} to a blank soft medium; false (nothing changed) if the medium is not blank or the file is of a kind it cannot take. */
    public static boolean write(ItemStack medium, ComputerFile file) {
        if (!blank(medium) || !file.fits())
            return false;
        switch (file.kind()) {
            case PROGRAM -> {
                ProgramGraph graph = graphOf(file);
                if (graph.nodes().isEmpty())
                    return false;
                medium.set(FDataComponents.PROGRAM_GRAPH, graph);
                // as a printed card: one of a kind
                medium.set(DataComponents.MAX_STACK_SIZE, 1);
            }
            case APP -> {
                if (!ComputerApps.DISK_APPS.contains(file.name()))
                    return false;
                medium.set(FDataComponents.APP.get(), file.name());
                medium.set(DataComponents.ITEM_NAME, Component.translatable("item.feedback.app." + file.name()));
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
