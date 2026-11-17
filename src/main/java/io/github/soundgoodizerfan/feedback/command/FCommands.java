// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularHeads;
import io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.bench.Station;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchShaping;
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.BarrelGeometry;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.Magazine;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test commands, op only: items that carry data no recipe browser can hand out -- a gun built
 * to a blueprint, the machined parts a blueprint lists, a barrel cut to any shape, rounds and
 * full magazines. A shortcut for testing crafting, never a route a survival player has.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FCommands {

    private static final SimpleCommandExceptionType NO_BLUEPRINT =
            new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_blueprint"));
    private static final SimpleCommandExceptionType NO_MODULAR =
            new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_modular"));
    private static final SimpleCommandExceptionType NO_MATERIAL =
            new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_material"));

    private static final SimpleCommandExceptionType NO_BATTERY =
            new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_battery"));

    private FCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("feedback").requires(s -> s.hasPermission(2))
                .then(Commands.literal("blueprint").executes(FCommands::blueprintOf))
                .then(Commands.literal("space")
                        .then(Commands.literal("probe").executes(FCommands::spaceProbe))
                        .then(Commands.literal("send").executes(c -> space(c, 0, 0)))
                        .then(Commands.literal("back").executes(c -> space(c, 1, 0)))
                        .then(Commands.literal("scale")
                                .then(Commands.literal("reset").executes(c -> space(c, 2, 1)))
                                .then(Commands.argument("k", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(
                                                io.github.soundgoodizerfan.feedback.space.SpaceTravel.minScale(),
                                                io.github.soundgoodizerfan.feedback.space.SpaceTravel.maxScale()))
                                        .executes(c -> space(c, 2, com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "k"))))))
                .then(Commands.literal("charge").executes(FCommands::charge))
                .then(Commands.literal("computers").executes(c -> computers(c, null, false))
                        .then(Commands.literal("purge")
                                .then(Commands.argument("id", StringArgumentType.word()).executes(c -> computers(c, StringArgumentType.getString(c, "id"), true)))))
                .then(Commands.literal("audit").then(Commands.literal("materials").executes(FCommands::auditMaterials)))
                .then(Commands.literal("build").executes(FCommands::build)
                        .then(Commands.literal("parts").executes(FCommands::parts)))
                .then(Commands.literal("part")
                        .then(Commands.argument("part", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(partNames(), b))
                                .then(Commands.argument("material", ResourceLocationArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(MaterialTable.get().materials().keySet(), b))
                                        .executes(FCommands::part))))
                .then(knowledge("learn", true))
                .then(knowledge("unlearn", false))
                .then(Commands.literal("summon").then(Commands.literal("gunskeleton").executes(c -> gunSkeleton(c, null))
                        .then(Commands.argument("gun", ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                        io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.get().earlyIds(), b))
                                .executes(c -> gunSkeleton(c, ResourceLocationArgument.getId(c, "gun"))))))
                .then(Commands.literal("barrel")
                        .then(Commands.argument("material", ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(MaterialTable.get().materials().keySet(), b))
                                .then(Commands.argument("bore", FloatArgumentType.floatArg(1f, 40f))
                                        .then(Commands.argument("wall", FloatArgumentType.floatArg(0.5f, 20f))
                                                .then(Commands.argument("length", FloatArgumentType.floatArg(0.05f, 2f))
                                                        .executes(FCommands::barrel))))))
                .then(Commands.literal("round")
                        .then(Commands.argument("bore", IntegerArgumentType.integer(GunSpec.MIN_BORE_MM, GunSpec.MAX_BORE_MM))
                                .executes(c -> round(c, false))
                                .then(Commands.argument("shot", BoolArgumentType.bool())
                                        .executes(c -> round(c, BoolArgumentType.getBool(c, "shot"))))))
                .then(Commands.literal("magazine")
                        .then(Commands.argument("size", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(MagazineSize.values()).map(MagazineSize::getSerializedName), b))
                                .then(Commands.argument("bore", IntegerArgumentType.integer(GunSpec.MIN_BORE_MM, GunSpec.MAX_BORE_MM))
                                        .executes(FCommands::magazine)))));
    }

    private static final SimpleCommandExceptionType NO_BODY =
            new SimpleCommandExceptionType(Component.translatable("commands.feedback.space.no_body"));

    /** What the crossing trigger sees for the body you stand on or look at. */
    private static int spaceProbe(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        var player = c.getSource().getPlayerOrException();
        var body = io.github.soundgoodizerfan.feedback.space.SpaceTravel.bodyOf(player);
        if (body == null)
            throw NO_BODY.create();
        var level = player.serverLevel();
        double y = body.logicalPose().position().y();
        var vel = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(level).physicsSystem()
                .getPhysicsHandle(body).getLinearVelocity(new org.joml.Vector3d());
        double speed = vel.length(), mass = body.getMassTracker().getMass();
        double density = io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable.at(level, y);
        org.joml.Vector3d along = speed < 1e-3 ? new org.joml.Vector3d(0, 1, 0)
                : body.logicalPose().transformNormalInverse(new org.joml.Vector3d(vel).div(speed), new org.joml.Vector3d());
        double area = io.github.soundgoodizerfan.feedback.vehicle.AirDrag.area(body, along);
        double k = io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning.AIR_DRAG;
        String line = String.format("y %.1f, density %.3f, held %d, speed %.1f b/s, mass %.0f, area %.2f blocks2, drag %.0f N, weight %.0f N, terminal %.0f b/s",
                y, density, io.github.soundgoodizerfan.feedback.space.SpaceTrigger.held(body.getUniqueId()), speed, mass, area,
                k * density * area * speed * speed, mass * io.github.soundgoodizerfan.feedback.vehicle.Gravity.of(level),
                Math.sqrt(mass * io.github.soundgoodizerfan.feedback.vehicle.Gravity.of(level) / Math.max(1e-6, k * density * area)));
        c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    /** {@code what}: 0 send, 1 back, 2 scale to {@code k}. Acts on the body the player stands on or looks at. */
    private static int space(CommandContext<CommandSourceStack> c, int what, double k) throws CommandSyntaxException {
        var player = c.getSource().getPlayerOrException();
        var body = io.github.soundgoodizerfan.feedback.space.SpaceTravel.bodyOf(player);
        if (body == null) throw NO_BODY.create();
        var level = player.serverLevel();
        if (what == 2) {
            io.github.soundgoodizerfan.feedback.space.SpaceTravel.scale(body, k);
            c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.space.scaled", k), true);
            return 1;
        }
        int n = what == 0 ? io.github.soundgoodizerfan.feedback.space.SpaceTravel.send(level, body)
                : io.github.soundgoodizerfan.feedback.space.SpaceTravel.back(level, body);
        if (n == -1) throw new SimpleCommandExceptionType(Component.translatable(
                what == 0 ? "commands.feedback.space.already_there" : "commands.feedback.space.not_there")).create();
        if (n == -2) throw new SimpleCommandExceptionType(Component.translatable("commands.feedback.space.no_gap")).create();
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.space.moved", n), true);
        return n;
    }

    /**
     * Testing: fills the held battery, or the cell in a held electric gun, to what it can hold now (capacity times
     * condition, cold and water shares), so nothing is overcharged and no cell wears.
     */
    private static int charge(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        boolean gun = !BatteryStack.is(held);
        ItemStack battery = gun ? io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun.cell(held) : held;
        net.minecraft.nbt.CompoundTag state = BatteryStack.state(battery);
        if (state == null)
            throw NO_BATTERY.create();
        net.minecraft.nbt.CompoundTag next = state.copy();
        next.putFloat("Charge", BatteryStack.effectiveCapacity(next));
        battery.set(FDataComponents.BATTERY.get(), next);
        if (gun)
            io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun.setCell(held, battery);
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.charged"), true);
        return 1;
    }

    /** Standard-shape part names: the part item's path (spear_head), and its short form (spear) where it ends in _head. */
    private static List<String> partNames() {
        List<String> out = new ArrayList<>();
        for (var o : PartBenchShaping.options(ModularTypes.known())) {
            if (!o.variant().name().isEmpty() || !(BuiltInRegistries.ITEM.get(o.shaping().item()) instanceof ModularPartItem))
                continue;
            String path = o.shaping().item().getPath();
            out.add(path);
            if (path.endsWith("_head"))
                out.add(path.substring(0, path.length() - 5));
        }
        return out;
    }

    /** Testing: a standard-shape part of any material that can make it, as the Tool Table would hand it over. */
    private static int part(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "part");
        ResourceLocation id = ResourceLocationArgument.getId(c, "material");
        Material m = MaterialTable.get().materials().get(id);
        var level = c.getSource().getLevel();
        for (var o : PartBenchShaping.options(ModularTypes.on(level))) {
            String path = o.shaping().item().getPath();
            if (!o.variant().name().isEmpty() || !(path.equals(name) || path.equals(name + "_head")))
                continue;
            for (String form : o.shaping().stock()) {
                if (m == null || form.contains("*") || m.item(id, form).isEmpty()
                        || !PartBenchShaping.takes(Station.TOOL, o, new Material.Located(id, m, form)))
                    continue;
                var stock = new PartBenchShaping.Stock(new Material.Located(id, m, form), o.cells(), o.stockMb(1f), 0f, 0);
                ItemStack part = PartBenchShaping.part(o, stock, 0, level).orElseThrow(NO_MATERIAL::create);
                GunAmmo.give(player, part);
                c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.part", part.getHoverName()), false);
                return 1;
            }
            throw NO_MATERIAL.create();
        }
        throw new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_part")).create();
    }

    private interface Run {
        int run(CommandContext<CommandSourceStack> c, java.util.Collection<ServerPlayer> players) throws CommandSyntaxException;
    }

    /** {@code node} runs on the caller, or on each player of a trailing selector. */
    private static <T extends com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, T>> T targets(T node, Run run) {
        return node.executes(c -> run.run(c, List.of(c.getSource().getPlayerOrException())))
                .then(Commands.argument("players", net.minecraft.commands.arguments.EntityArgument.players())
                        .executes(c -> run.run(c, net.minecraft.commands.arguments.EntityArgument.getPlayers(c, "players"))));
    }

    /** Testing: {@code learn} gives a perfect rite of an attunement or teaches a potion effect node by name, or {@code all} of either; {@code unlearn} forgets potion nodes. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> knowledge(String name, boolean learn) {
        var attunement = Commands.literal("attunement")
                .then(targets(Commands.literal("all"), (c, ps) -> attunements(c, ps, null, learn)))
                .then(targets(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                io.github.soundgoodizerfan.feedback.modular.attunement.Attunements.get().effects().keySet().stream().map(ResourceLocation::getPath), b)),
                        (c, ps) -> attunements(c, ps, StringArgumentType.getString(c, "name"), learn)));
        var potion = Commands.literal("potion")
                .then(targets(Commands.literal("all"), (c, ps) -> potions(c, ps, null, learn)))
                .then(targets(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(potionNodes().stream().map(ResourceLocation::getPath), b)),
                        (c, ps) -> potions(c, ps, StringArgumentType.getString(c, "name"), learn)));
        return learn ? Commands.literal(name).then(attunement).then(potion) : Commands.literal(name).then(potion);
    }

    private static List<ResourceLocation> potionNodes() {
        var out = new ArrayList<ResourceLocation>();
        for (var e : io.github.soundgoodizerfan.feedback.potion.PotionMap.get().on("water"))
            if (e.getValue().kind() == io.github.soundgoodizerfan.feedback.potion.PotionFeature.Kind.EFFECT)
                out.add(e.getKey());
        return out;
    }

    private static int attunements(CommandContext<CommandSourceStack> c, java.util.Collection<ServerPlayer> players, String named, boolean learn) throws CommandSyntaxException {
        var all = io.github.soundgoodizerfan.feedback.modular.attunement.Attunements.get().effects().values();
        var picked = all.stream().filter(e -> named == null || e.id().getPath().equals(named)).toList();
        if (picked.isEmpty())
            throw new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_such", named)).create();
        // Testing: "learn" hands over a perfect rite of the effect; there is nothing learned on a player to forget.
        for (ServerPlayer p : players)
            for (var e : picked)
                p.getInventory().placeItemBackInInventory(io.github.soundgoodizerfan.feedback.modular.attunement.Rites.of(e.id(), 1f));
        return done(c, players, "attunement " + (named == null ? "all" : named), learn);
    }

    private static int potions(CommandContext<CommandSourceStack> c, java.util.Collection<ServerPlayer> players, String named, boolean learn) throws CommandSyntaxException {
        var map = io.github.soundgoodizerfan.feedback.potion.PotionMap.get().on("water");
        ResourceLocation node = null;
        if (named != null) {
            node = potionNodes().stream().filter(id -> id.getPath().equals(named)).findFirst()
                    .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_such", named)).create());
        }
        var type = io.github.soundgoodizerfan.feedback.potion.PotionAttachments.KNOWLEDGE.get();
        for (ServerPlayer p : players) {
            var now = p.getData(type);
            p.setData(type, named == null
                    ? (learn ? io.github.soundgoodizerfan.feedback.potion.PotionKnowledge.everything(map) : io.github.soundgoodizerfan.feedback.potion.PotionKnowledge.EMPTY)
                    : (learn ? now.made(node) : now.forgot(node)));
        }
        return done(c, players, "potion " + (named == null ? "all" : named), learn);
    }

    private static int done(CommandContext<CommandSourceStack> c, java.util.Collection<ServerPlayer> players, String what, boolean learn) {
        c.getSource().sendSuccess(() -> Component.translatable(learn ? "commands.feedback.learned" : "commands.feedback.unlearned", what, players.size()), true);
        return players.size();
    }

    /** A skeleton at the source, always armed from the early pool (the named entry, else a random one); no chance roll. */
    private static int gunSkeleton(CommandContext<CommandSourceStack> c, ResourceLocation named) throws CommandSyntaxException {
        var level = c.getSource().getLevel();
        var pool = io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.get();
        var picked = named == null ? pool.drawEarly(level.random)
                : pool.early(named);
        var skeleton = net.minecraft.world.entity.EntityType.SKELETON.create(level);
        if (skeleton == null || !io.github.soundgoodizerfan.feedback.weapon.ArmedSkeletons.equip(skeleton, picked))
            throw new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_gun_skeleton")).create();
        skeleton.moveTo(c.getSource().getPosition());
        level.addFreshEntity(skeleton);
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.gun_skeleton"), false);
        return 1;
    }

    private static ItemStack held(ServerPlayer player) throws CommandSyntaxException {
        ItemStack held = player.getMainHandItem();
        if (!held.is(io.github.soundgoodizerfan.feedback.registry.FModular.BLUEPRINT.get()) || ModularStacks.design(held) == null)
            throw NO_BLUEPRINT.create();
        return held;
    }

    /**
     * The item built exactly to the held blueprint, whatever it is: a gun, a pickaxe, a crossbow.
     * One blueprint item and one command, because a gun is a modular type like the rest (spec 8.3).
     */
    private static int build(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack blueprint = held(player);
        GunAmmo.give(player, toolFor(blueprint));
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.build"), false);
        return 1;
    }

    /** A blueprint of the held modular item: its design and figures, under its name. */
    private static int blueprintOf(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        ModularDesign design = ModularStacks.design(held);
        if (design == null || held.is(io.github.soundgoodizerfan.feedback.registry.FModular.BLUEPRINT.get()))
            throw NO_MODULAR.create();
        ItemStack out = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FModular.BLUEPRINT.get());
        out.set(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get(), design);
        var figures = io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns.predict(design, ModularTypes.get().types(), MaterialTable.get().materials());
        figures.ifPresent(f -> out.set(io.github.soundgoodizerfan.feedback.registry.FModular.FIGURES.get(), f));
        var named = held.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (named != null)
            out.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, named);
        GunAmmo.give(player, out);
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.blueprint"), false);
        return 1;
    }

    /** A modular item built exactly to {@code blueprint}'s design, figures baked, under its name. */
    public static ItemStack toolFor(ItemStack blueprint) throws CommandSyntaxException {
        ModularDesign design = ModularStacks.design(blueprint);
        // The item the Tool Table makes from this design's parts: the blueprint and the table cannot disagree.
        ItemStack out = design == null ? ItemStack.EMPTY
                : io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns.build(design, ModularTypes.get().types(), MaterialTable.get().materials()).orElse(ItemStack.EMPTY);
        if (out.isEmpty())
            throw NO_BLUEPRINT.create();
        var named = blueprint.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (named != null)
            out.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, named);
        return out;
    }

    /** The parts a modular design assembles from: a shaped part carries what it is made of, raw stock is its cells. */
    public static List<ItemStack> toolPartsFor(ModularDesign design) throws CommandSyntaxException {
        ModularType type = ModularTypes.get().types().get(design.type());
        var materials = MaterialTable.get().materials();
        List<ItemStack> out = new ArrayList<>();
        if (type == null)
            throw NO_BLUEPRINT.create();
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null || m == null)
                throw NO_BLUEPRINT.create();
            if (role.part().isPresent() || role.striker()) {
                String kind = role.striker() ? ModularHeads.kindOf(p, role) : role.part().get().item().getPath();
                ItemStack part = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Feedback.id(kind)));
                part.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(),
                        new io.github.soundgoodizerfan.feedback.process.MadeOf(p.material(), p.form(), p.count(), p.variant(), p.flaw(), p.measures()));
                out.add(part);
            } else {
                ItemStack stock = m.stack(p.material(), p.form()).orElseThrow(NO_MATERIAL::create);
                for (int k = 0; k < p.count(); k++)
                    out.add(stock.copy());
            }
        }
        return out;
    }

    /** Every part the held blueprint lists, in its planned materials. */
    /** Dev: writes {@code material_audit.txt} in the run directory, one line per part or slot saying whether its material changes any figure. */
    private static int auditMaterials(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        List<String> lines = io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns.materialAudit(ModularTypes.get().types(), MaterialTable.get().materials());
        try {
            java.nio.file.Files.write(java.nio.file.Path.of("material_audit.txt"), lines);
        } catch (java.io.IOException e) {
            throw new SimpleCommandExceptionType(Component.literal(e.getMessage())).create();
        }
        long dead = lines.stream().filter(l -> l.endsWith("DEAD")).count();
        c.getSource().sendSuccess(() -> Component.literal(lines.size() + " places audited, " + dead + " dead: material_audit.txt"), false);
        return lines.size();
    }

    private static int parts(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack blueprint = held(player);
        List<ItemStack> out = toolPartsFor(ModularStacks.design(blueprint));
        out.forEach(s -> GunAmmo.give(player, s));
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.parts", out.size()), false);
        return out.size();
    }

    private static int barrel(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ResourceLocation metal = ResourceLocationArgument.getId(c, "material");
        float bore = FloatArgumentType.getFloat(c, "bore"), wall = FloatArgumentType.getFloat(c, "wall");
        ItemStack barrel = form(MaterialTable.get().materials(), metal, "gun_barrel");
        barrel.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(), io.github.soundgoodizerfan.feedback.process.Machining.withGeometry(
                barrel.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get()),
                new BarrelGeometry(bore, bore + 2 * wall, FloatArgumentType.getFloat(c, "length"))));
        GunAmmo.give(player, barrel);
        c.getSource().sendSuccess(() -> Component.translatable("commands.feedback.barrel"), false);
        return 1;
    }

    private static int round(CommandContext<CommandSourceStack> c, boolean shot) throws CommandSyntaxException {
        int bore = IntegerArgumentType.getInteger(c, "bore");
        GunAmmo.give(c.getSource().getPlayerOrException(),
                GunAmmo.loose(shot ? Cartridge.referenceShot(bore) : Cartridge.reference(bore)).copyWithCount(16));
        return 1;
    }

    private static int magazine(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        MagazineSize size = Arrays.stream(MagazineSize.values())
                .filter(m -> m.getSerializedName().equals(StringArgumentType.getString(c, "size"))).findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("commands.feedback.no_size")).create());
        ItemStack mag = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
        mag.set(FWeapons.MAGAZINE_SIZE.get(), size);
        mag.set(FWeapons.MAGAZINE.get(), new Magazine(Collections.nCopies(size.capacity(),
                Cartridge.reference(IntegerArgumentType.getInteger(c, "bore")))));
        GunAmmo.give(c.getSource().getPlayerOrException(), mag);
        return 1;
    }

    /** {@code metal}'s item in {@code form}, through the material system. */
    private static ItemStack form(java.util.Map<ResourceLocation, Material> materials, ResourceLocation metal, String form)
            throws CommandSyntaxException {
        Material m = materials.get(metal);
        var stack = m == null ? java.util.Optional.<ItemStack>empty() : m.stack(metal, form);
        if (stack.isEmpty())
            throw NO_MATERIAL.create();
        return stack.get();
    }

    /** Lists the internal disks of every Computer (placed or carried, last seen, files, bytes), or purges the one whose id starts with {@code id}. */
    private static int computers(CommandContext<CommandSourceStack> c, String id, boolean purge) {
        var store = io.github.soundgoodizerfan.feedback.control.computer.ComputerStore.get(c.getSource().getServer());
        long now = c.getSource().getServer().overworld().getGameTime();
        var rows = new java.util.ArrayList<java.util.Map.Entry<java.util.UUID, io.github.soundgoodizerfan.feedback.control.computer.ComputerStore.Entry>>(store.all().entrySet());
        // carried and long unseen first: those are the ones worth purging
        rows.sort(java.util.Comparator.comparing((java.util.Map.Entry<java.util.UUID, io.github.soundgoodizerfan.feedback.control.computer.ComputerStore.Entry> e) -> !e.getValue().carried())
                .thenComparingLong(e -> e.getValue().lastSeen()));
        if (purge) {
            java.util.List<java.util.UUID> hits = rows.stream().map(java.util.Map.Entry::getKey).filter(u -> u.toString().startsWith(id)).toList();
            if (hits.size() != 1) {
                c.getSource().sendFailure(Component.literal(hits.isEmpty() ? "No computer disk starts with " + id : "Several disks start with " + id));
                return 0;
            }
            if (store.isClaimed(hits.get(0))) {
                c.getSource().sendFailure(Component.literal("That disk is in a loaded Computer"));
                return 0;
            }
            store.purge(hits.get(0));
            c.getSource().sendSuccess(() -> Component.literal("Purged " + hits.get(0)), true);
            return 1;
        }
        for (var e : rows) {
            var entry = e.getValue();
            String where = entry.carried() ? "carried" : store.isClaimed(e.getKey()) ? "placed, loaded" : "placed";
            c.getSource().sendSuccess(() -> Component.literal(e.getKey().toString().substring(0, 8) + " " + where + ", "
                    + entry.files().size() + " files, " + entry.bytes() + " bytes, last seen " + Math.max(0, now - entry.lastSeen()) / 1200 + " min ago"), false);
        }
        c.getSource().sendSuccess(() -> Component.literal(rows.size() + " computer disks"), false);
        return rows.size();
    }
}
