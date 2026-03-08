package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoLogin extends Module {
    public static AutoLogin INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> password = sgGeneral.add(new StringSetting.Builder()
        .name("Password")
        .description("Your login password.")
        .defaultValue("")
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("Delay before sending login command (ms).")
        .defaultValue(1000)
        .min(0)
        .sliderMax(5000)
        .build()
    );

    private boolean hasLoggedIn = false;

    public AutoLogin() {
        super(LeavesHack.CATEGORY, "AutoLogin", "Automatically logs you into the server.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        hasLoggedIn = false;
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (hasLoggedIn) return;

        String message = event.getMessage().getString().toLowerCase();

        if (message.contains("login") || message.contains("register") || message.contains("authentication")) {
            if (password.get().isEmpty()) {
                error("Password is not set!");
                return;
            }

            new Thread(() -> {
                try {
                    Thread.sleep(delay.get());
                    mc.player.networkHandler.sendChatMessage("/login " + password.get());
                    hasLoggedIn = true;
                    info("Logged in successfully!");
                } catch (InterruptedException e) {
                    error("Login interrupted!");
                }
            }).start();
        }
    }
}
