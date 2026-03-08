package com.dev.leavesHack;

import com.dev.leavesHack.commands.CommandExample;
import com.dev.leavesHack.hud.HudExample;
import com.dev.leavesHack.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class LeavesHack extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("LeavesHack");
    public static final HudGroup HUD_GROUP = new HudGroup("LeavesHack");

    @Override
    public void onInitialize() {
        LOG.info("Initializing LeavesHack V1.3.0 by Leaves_aws | 1.21.4 Updated by kangleyao");

        // Modules
        Modules.get().add(new AutoCity());
        Modules.get().add(new AutoPlaceSlab());
        Modules.get().add(new NukerPlus());
        Modules.get().add(new ModuleList());
        Modules.get().add(new PacketMine());
        Modules.get().add(new Aura());
        Modules.get().add(new AutoArmorPlus());
        Modules.get().add(new FireworkElytraFly());
        Modules.get().add(new AntiAntiXray());
        Modules.get().add(new AutoRefreshTrade());
        Modules.get().add(new AutoBackdoor());

        // V1.3.0 New Modules
        Modules.get().add(new AutoLogin());
        Modules.get().add(new AutoTorch());
        Modules.get().add(new LegitNoFall());
        Modules.get().add(new Printer());
        Modules.get().add(new ScaffoldPlus());

        // Commands
        Commands.add(new CommandExample());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }
    @Override
    public String getPackage() {
        return "com.dev.leavesHack";
    }
    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
