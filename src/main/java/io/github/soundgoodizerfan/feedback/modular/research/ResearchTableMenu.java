// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.List;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerApps;
import io.github.soundgoodizerfan.feedback.modular.attunement.Rites;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringAssembly;
import io.github.soundgoodizerfan.feedback.modular.engineering.TreatiseGame;
import io.github.soundgoodizerfan.feedback.modular.item.ModularRework;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;
import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.net.ResearchOpenPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Research Table: a Blank Treatise and a specimen. the tabs choose between researching a treatise volume, an attunement written onto a
 * rite and an app written onto a blank disk (a Floppy Disk with nothing on it and the thing the app is about), a click on a listed entry picks it, {@link #BTN_RESEARCH} opens the research board. A volume takes as its specimen an item
 * the volume before it taught how to build (a first volume, one of the plain things its plan names) and wants that earlier treatise in hand.
 * The experience is spent whatever the score; the specimen is worn or comes apart by how hard the researcher pushed it; the blank becomes the
 * volume with the score as its notes' quality, or the attunement is learned.
 */
public class ResearchTableMenu extends AbstractContainerMenu {

    /** {@code BTN_TREATISES} and {@code BTN_ATTUNEMENTS} choose what is listed, {@code BTN_PICK + i} picks the i-th entry, {@code BTN_RESEARCH} opens the board. */
    public static final int BTN_TREATISES = 1, BTN_ATTUNEMENTS = 2, BTN_RESEARCH = 3, BTN_APPS = 4, BTN_PICK = 100;
    public static final int NOTE_NONE = 0, NOTE_NO_BLANK = 1, NOTE_NO_SPECIMEN = 2, NOTE_NO_XP = 3, NOTE_NOTHING = 4, NOTE_WRITTEN = 5, NOTE_LEARNED = 6,
            NOTE_NO_EARLIER = 7, NOTE_NO_PLAN = 8, NOTE_NO_OFFERING = 9, NOTE_NO_DISK = 10, NOTE_NO_SUBJECT = 11, NOTE_APP_WRITTEN = 12, NOTE_APP_FAILED = 13;

    public static final int W = 304, H = 298, PAGE_X = 156, BLANK_X = 160, SPECIMEN_X = 214, RITE_X = 268, SLOT_Y = 170, INV_X = 71, INV_Y = 212;

    private final Container table;
    private final DataSlot mode = DataSlot.standalone(), index = DataSlot.standalone(), note = DataSlot.standalone();
    private final Player player;
    private final BlockPos pos;
    private boolean playing;
    private long gameStart;
    private int sentMode = -1, sentIndex = -1, sentInk, sentSeed;
    private ResearchPlan sentPlan;

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
            public boolean isActive() {
                return mode.get() == 0;
            }

            @Override
            public boolean mayPlace(ItemStack s) {
                return s.is(FModular.BLANK_TREATISE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(table, ResearchTableBlockEntity.SPECIMEN, SPECIMEN_X, SLOT_Y) {
            @Override
            public boolean isActive() {
                return mode.get() == 0;
            }

            @Override
            public boolean mayPlace(ItemStack s) {
                return ModularStacks.design(s) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(table, ResearchTableBlockEntity.OFFERING, BLANK_X, SLOT_Y) {
            @Override
            public boolean isActive() {
                return mode.get() == 1;
            }

            @Override
            public boolean mayPlace(ItemStack s) {
                return Researching.rank("hardness", s) >= 0f || Researching.rank("density", s) >= 0f;
            }
        });
        addSlot(new Slot(table, ResearchTableBlockEntity.REAGENT, SPECIMEN_X, SLOT_Y) {
            @Override
            public boolean isActive() {
                return mode.get() == 1;
            }

            @Override
            public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof io.github.soundgoodizerfan.feedback.potion.PotionItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(table, ResearchTableBlockEntity.BLANK_RITE, RITE_X, SLOT_Y) {
            @Override
            public boolean isActive() {
                return mode.get() == 1;
            }

            @Override
            public boolean mayPlace(ItemStack s) {
                return s.is(FModular.BLANK_RITE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(table, ResearchTableBlockEntity.BLANK_DISK, BLANK_X, SLOT_Y) {
            @Override
            public boolean isActive() {
                return mode.get() == 2;
            }

            @Override
            public boolean mayPlace(ItemStack s) {
                return Researching.blankDisk(s);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        // the client holds no plans, so any item may be set down; what the app is about is checked when the research starts
        addSlot(new Slot(table, ResearchTableBlockEntity.SUBJECT, SPECIMEN_X, SLOT_Y) {
            @Override
            public boolean isActive() {
                return mode.get() == 2;
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

    private ContainerLevelAccess access() {
        return ContainerLevelAccess.create(player.level(), pos);
    }

    /** The attunement picked in attunement mode (the table has checked there is one). */
    private io.github.soundgoodizerfan.feedback.modular.attunement.Attunements.Effect effect(ServerPlayer server) {
        var list = Researching.effects();
        return list.get(Math.floorMod(index.get(), list.size()));
    }

    /** 0 when a run can start, else the note. */
    private int ready(ServerPlayer server) {
        if (Researching.count(mode.get()) == 0)
            return NOTE_NOTHING;
        if (mode.get() == 0 && !table.getItem(ResearchTableBlockEntity.BLANK).is(FModular.BLANK_TREATISE.get()))
            return NOTE_NO_BLANK;
        if (mode.get() == 1 && !table.getItem(ResearchTableBlockEntity.BLANK_RITE).is(FModular.BLANK_RITE.get()))
            return NOTE_NO_BLANK;
        if (mode.get() == 2 && !Researching.blankDisk(table.getItem(ResearchTableBlockEntity.BLANK_DISK)))
            return NOTE_NO_DISK;
        if (Researching.plan(mode.get(), index.get()) == null)
            return NOTE_NO_PLAN;
        if (!Researching.builtOn(server, access(), mode.get(), index.get()))
            return NOTE_NO_EARLIER;
        if (mode.get() == 2 && !Researching.subjectFits(Researching.plan(2, index.get()), table.getItem(ResearchTableBlockEntity.SUBJECT)))
            return NOTE_NO_SUBJECT;
        if (mode.get() == 0 && !Researching.specimenFits(table.getItem(ResearchTableBlockEntity.SPECIMEN), mode.get(), index.get()))
            return NOTE_NO_SPECIMEN;
        if (mode.get() == 1 && Researching.attunementInk(effect(server), table.getItem(ResearchTableBlockEntity.OFFERING),
                table.getItem(ResearchTableBlockEntity.REAGENT), server.level()) < 0)
            return NOTE_NO_OFFERING;
        if (Researching.points(server) < ModularTuning.RESEARCH_XP && !server.getAbilities().instabuild)
            return NOTE_NO_XP;
        return 0;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer server))
            return false;
        if (id == BTN_TREATISES || id == BTN_ATTUNEMENTS || id == BTN_APPS) {
            mode.set(id == BTN_APPS ? 2 : id == BTN_ATTUNEMENTS ? 1 : 0);
            index.set(0);
            note.set(0);
            return true;
        }
        if (id >= BTN_PICK) {
            int count = Researching.count(mode.get());
            if (id - BTN_PICK < count) {
                index.set(id - BTN_PICK);
                note.set(0);
            }
            return true;
        }
        if (id != BTN_RESEARCH)
            return false;
        int why = ready(server);
        note.set(why);
        if (why != 0)
            return true;
        long now = server.level().getGameTime();
        if (playing && now - gameStart < 20)
            return true;
        ResearchPlan plan = Researching.plan(mode.get(), index.get());
        playing = true;
        gameStart = now;
        sentMode = mode.get();
        sentIndex = index.get();
        sentPlan = plan;
        sentInk = mode.get() == 2 ? Researching.appInk(server, plan) : mode.get() == 1 ? Researching.attunementInk(effect(server), table.getItem(ResearchTableBlockEntity.OFFERING),
                table.getItem(ResearchTableBlockEntity.REAGENT), server.level())
                : Researching.ink(server, table.getItem(ResearchTableBlockEntity.SPECIMEN), plan);
        sentSeed = (int) (now ^ containerId * 31L);
        PacketDistributor.sendToPlayer(server, new ResearchOpenPayload(containerId, plan.json(), sentInk, sentSeed));
        return true;
    }

    /**
     * The client's one result: the placements it made and the answer it gave, which are replayed here over the plan, seed and ink that were
     * sent ({@link ResearchBoard}); the score and the strain are what that replay makes, never what the client says. A result sent faster than an
     * honest play could be is scored as nothing. The experience is spent; the specimen is worn or comes apart; the volume is written or the
     * attunement learned.
     */
    public void finishResearch(ServerPlayer server, List<String> moves, int answer) {
        if (!playing || sentMode != mode.get() || sentIndex != index.get() || sentPlan == null)
            return;
        playing = false;
        long elapsed = server.level().getGameTime() - gameStart;
        if (mode.get() == 2) {
            AppHack hack = AppHack.replay(sentPlan, sentSeed, sentInk, moves);
            if (ready(server) != 0)
                note.set(ready(server));
            else
                writeApp(server, hack.found && elapsed >= ModularTuning.RESEARCH_MIN_TICKS);
            table.setChanged();
            broadcastChanges();
            return;
        }
        ResearchBoard board = new ResearchBoard(sentPlan, sentSeed, sentInk);
        for (String move : moves) {
            int at = move.indexOf('@'), comma = move.indexOf(',', at + 1);
            if (at < 0 || comma < 0 || board.over())
                break;
            try {
                board.place(move.substring(0, at), ResearchBoard.key(Integer.parseInt(move.substring(at + 1, comma)), Integer.parseInt(move.substring(comma + 1))));
            } catch (NumberFormatException e) {
                break;
            }
        }
        float quality = elapsed < ModularTuning.RESEARCH_MIN_TICKS ? 0f : board.quality(board.reached && answer == sentPlan.answer);
        int pushed = mode.get() == 0 ? board.strain(Researching.forgiving(table.getItem(ResearchTableBlockEntity.SPECIMEN))) : 0;
        int why = ready(server);
        if (why != 0) {
            note.set(why);
            return;
        }
        if (!server.getAbilities().instabuild)
            server.giveExperiencePoints(-ModularTuning.RESEARCH_XP);
        if (mode.get() == 0)
            strainSpecimen(server, pushed);
        else {
            table.getItem(ResearchTableBlockEntity.OFFERING).shrink(1);
            table.setItem(ResearchTableBlockEntity.REAGENT, ItemStack.EMPTY);
        }
        if (mode.get() == 0) {
            int[] v = Researching.volume(sentIndex);
            School school = School.values()[v[0]];
            table.setItem(ResearchTableBlockEntity.BLANK, TreatiseGame.onLearn(server, school, v[1], ItemStack.EMPTY, quality));
            HudReadoutPayload.send(server, Component.translatable("message.feedback.treatise.written", Component.translatable(school.itemKey(v[1]))), 60);
            note.set(NOTE_WRITTEN);
        } else {
            List<io.github.soundgoodizerfan.feedback.modular.attunement.Attunements.Effect> list = Researching.effects();
            var effect = list.get(Math.floorMod(sentIndex, list.size()));
            table.setItem(ResearchTableBlockEntity.BLANK_RITE, Rites.of(effect.id(), quality));
            HudReadoutPayload.send(server, Component.translatable("message.feedback.research.learned", Component.translatable("attunement.feedback." + effect.id().getPath())), 60);
            note.set(NOTE_LEARNED);
        }
        table.setChanged();
        broadcastChanges();
    }

    /**
     * An app's research ends in a disk or in nothing: a chain that reached its goal in an honest time writes the app onto the blank; what the app is
     * about is only studied, never used up, and a run that did not leaves the blank alone (the experience is gone either way).
     */
    private void writeApp(ServerPlayer server, boolean learned) {
        if (!server.getAbilities().instabuild)
            server.giveExperiencePoints(-ModularTuning.RESEARCH_XP);
        if (!learned) {
            note.set(NOTE_APP_FAILED);
            return;
        }
        String app = Researching.app(sentIndex);
        table.setItem(ResearchTableBlockEntity.BLANK_DISK, ComputerApps.disk(app));
        HudReadoutPayload.send(server, Component.translatable("message.feedback.research.app", Component.translatable("item.feedback.app." + app)), 60);
        note.set(NOTE_APP_WRITTEN);
    }

    /** Pushing the specimen hard wears it, and pushing it too hard takes it apart into its parts, which go to the researcher. */
    private void strainSpecimen(ServerPlayer server, int strain) {
        ItemStack specimen = table.getItem(ResearchTableBlockEntity.SPECIMEN);
        if (specimen.isEmpty() || strain < ModularTuning.RESEARCH_WEAR_STRAIN)
            return;
        if (strain >= ModularTuning.RESEARCH_BREAK_STRAIN) {
            var level = server.level();
            List<ItemStack> parts = ModularRework.dismantle(specimen, ClientMaterials.on(level), EngineeringAssembly.types(level), 0f);
            table.setItem(ResearchTableBlockEntity.SPECIMEN, ItemStack.EMPTY);
            parts.forEach(p -> server.getInventory().placeItemBackInInventory(p));
            return;
        }
        if (specimen.isDamageableItem()) {
            int cost = Math.round(specimen.getMaxDamage() * ModularTuning.RESEARCH_WEAR_SHARE * strain / 100f);
            specimen.setDamageValue(Math.min(specimen.getMaxDamage() - 1, specimen.getDamageValue() + cost));
            ModularStacks.refresh(specimen);
        }
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
