// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.attunement.AttunementKnowledge;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringMinigames;
import io.github.soundgoodizerfan.feedback.modular.treatise.ResearchTheme;
import io.github.soundgoodizerfan.feedback.modular.engineering.TreatiseGame;
import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Research Table: a Blank Treatise and up to four samples of stuff on the bench. {@link #BTN_MODE} turns between researching a treatise
 * volume and an attunement not yet known, {@link #BTN_NEXT} steps through what there is, {@link #BTN_RESEARCH} opens the research game
 * ({@code ResearchScreen}). The experience is spent and the samples used up whatever the score; the blank becomes the volume, or the attunement
 * is learned, with the score as the quality of the notes (the treatise) or simply known (an attunement).
 */
public class ResearchTableMenu extends AbstractContainerMenu {

    public static final int BTN_MODE = 1, BTN_NEXT = 2, BTN_RESEARCH = 3;
    public static final int NOTE_NONE = 0, NOTE_NO_BLANK = 1, NOTE_NO_SAMPLES = 2, NOTE_NO_XP = 3, NOTE_NOTHING = 4, NOTE_WRITTEN = 5, NOTE_LEARNED = 6;

    public static final int W = 176, H = 196, BLANK_X = 18, SLOT_Y = 24, SAMPLE_X = 60, INV_X = 8, INV_Y = 114;

    private final Container table;
    private final DataSlot mode = DataSlot.standalone(), index = DataSlot.standalone(), note = DataSlot.standalone();
    private final Player player;
    private final BlockPos pos;
    private String gameId = "";
    private long gameStart;
    private int sentMode = -1, sentIndex = -1;

    public ResearchTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(ResearchTableBlockEntity.SIZE), buf.readBlockPos());
    }

    public ResearchTableMenu(int id, Inventory inventory, ResearchTableBlockEntity table) {
        this(id, inventory, table, table.getBlockPos());
    }

    private ResearchTableMenu(int id, Inventory inventory, Container table, BlockPos pos) {
        super(FMenus.RESEARCH_TABLE.get(), id);
        this.table = table;
        this.player = inventory.player;
        this.pos = pos;
        addSlot(new Slot(table, ResearchTableBlockEntity.BLANK, BLANK_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return s.is(FModular.BLANK_TREATISE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < ResearchTableBlockEntity.SAMPLE_COUNT; i++)
            addSlot(new Slot(table, ResearchTableBlockEntity.SAMPLES + i, SAMPLE_X + i * 18, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    return Material.find(io.github.soundgoodizerfan.feedback.process.ClientMaterials.on(player.level()), s).isPresent();
                }

                @Override
                public int getMaxStackSize() {
                    return 64;
                }
            });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
        addDataSlot(mode);
        addDataSlot(index);
        addDataSlot(note);
    }

    public int mode() {
        return mode.get();
    }

    public int index() {
        return index.get();
    }

    public int note() {
        return note.get();
    }

    /** The subject of what is picked now. */
    public ResearchTheme.Theme theme() {
        return Researching.theme(player, mode.get(), index.get());
    }

    private List<ItemStack> sampleStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < ResearchTableBlockEntity.SAMPLE_COUNT; i++)
            out.add(table.getItem(ResearchTableBlockEntity.SAMPLES + i));
        return out;
    }

    /** 0 when a run can start, else the note. */
    private int ready(ServerPlayer server) {
        if (mode.get() == 0 && !table.getItem(ResearchTableBlockEntity.BLANK).is(FModular.BLANK_TREATISE.get()))
            return NOTE_NO_BLANK;
        if (Researching.count(server, mode.get()) == 0)
            return NOTE_NOTHING;
        if (Researching.samples(sampleStacks(), theme()).size() < 3)
            return NOTE_NO_SAMPLES;
        if (server.totalExperience < ModularTuning.RESEARCH_XP && !server.getAbilities().instabuild)
            return NOTE_NO_XP;
        return 0;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer server))
            return false;
        if (id == BTN_MODE) {
            mode.set(1 - mode.get());
            index.set(0);
            note.set(0);
            return true;
        }
        int count = Researching.count(server, mode.get());
        if (id == BTN_NEXT) {
            index.set(count == 0 ? 0 : Math.floorMod(index.get() + 1, count));
            note.set(0);
            return true;
        }
        if (id != BTN_RESEARCH)
            return false;
        int why = ready(server);
        note.set(why);
        if (why != 0)
            return true;
        long now = server.level().getGameTime();
        if (!gameId.isEmpty() && now - gameStart < 20)
            return true;
        gameId = EngineeringMinigames.RESEARCH.toString();
        gameStart = now;
        sentMode = mode.get();
        sentIndex = index.get();
        var theme = theme();
        PacketDistributor.sendToPlayer(server, new MinigameOpenPayload(containerId, gameId, 1, 1,
                Researching.data(theme, Researching.samples(sampleStacks(), theme))));
        return true;
    }

    /** The client's one result: the notes' quality, 0 to 100 percent; a result sent faster than an honest play could be is scored as nothing. */
    public void finishMinigame(ServerPlayer server, int percent) {
        if (gameId.isEmpty() || sentMode != mode.get() || sentIndex != index.get())
            return;
        long elapsed = server.level().getGameTime() - gameStart;
        gameId = "";
        float score = elapsed < ModularTuning.RESEARCH_MIN_TICKS ? 0f : Math.max(0, Math.min(100, percent)) / 100f;
        int why = ready(server);
        if (why != 0) {
            note.set(why);
            return;
        }
        var theme = theme();
        for (ItemStack sample : Researching.samples(sampleStacks(), theme))
            sample.shrink(1);
        if (!server.getAbilities().instabuild)
            server.giveExperiencePoints(-ModularTuning.RESEARCH_XP);
        if (mode.get() == 0) {
            table.setItem(ResearchTableBlockEntity.BLANK, TreatiseGame.onLearn(server, theme.school(), theme.volume(), ItemStack.EMPTY, score));
            HudReadoutPayload.send(server, Component.translatable("message.feedback.treatise.written", Component.translatable(theme.school().itemKey(theme.volume()))), 60);
            note.set(NOTE_WRITTEN);
        } else {
            var list = Researching.unknown(server);
            var effect = list.get(Math.floorMod(sentIndex, list.size()));
            AttunementKnowledge.learn(server, effect);
            HudReadoutPayload.send(server, Component.translatable("message.feedback.research.learned", Component.translatable(effect.key())), 60);
            note.set(NOTE_LEARNED);
            index.set(0);
        }
        table.setChanged();
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        int tableEnd = ResearchTableBlockEntity.SIZE;
        if (slotIndex < tableEnd) {
            if (!moveItemStackTo(held, tableEnd, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(held, 0, tableEnd, false))
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player p) {
        return table.stillValid(p);
    }
}
