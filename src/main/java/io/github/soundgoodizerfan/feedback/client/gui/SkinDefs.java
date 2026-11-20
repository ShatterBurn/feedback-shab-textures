// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonParser;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * The data skins' definitions, read from {@code assets/feedback/gui_skin/*.json} at every resource reload (F3+T), so an artist edits
 * a sprite or a colour and sees it without recompiling. A file that fails to parse is logged and leaves that skin on its last good
 * definition.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SkinDefs {

    private static final Map<String, SkinDef> DEFS = new HashMap<>();

    private SkinDefs() {
    }

    /** The current definition of data skin {@code name}, or null before its file has loaded. */
    static SkinDef get(String name) {
        return DEFS.get(name);
    }

    @SubscribeEvent
    public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) SkinDefs::load);
    }

    private static void load(ResourceManager rm) {
        rm.listResources("gui_skin", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            if (!id.getNamespace().equals(Feedback.MOD_ID))
                return;
            String path = id.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            try (Reader reader = resource.openAsReader()) {
                DEFS.put(name, SkinDef.parse(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception e) {
                Feedback.LOGGER.error("GUI skin {} did not load, keeping the last good one", id, e);
            }
        });
        SpriteSkin.reloaded();
    }
}
