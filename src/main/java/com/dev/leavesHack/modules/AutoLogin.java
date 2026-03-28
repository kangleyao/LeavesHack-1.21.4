package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.modules.autoLogin.AutoLoginAccount;
import com.dev.leavesHack.modules.autoLogin.AutoLoginAccounts;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

import java.util.List;

public class AutoLogin extends Module {
    public static AutoLogin INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> autoSave = sgGeneral.add(new BoolSetting.Builder()
        .name("AutoSave")
        .description("Automatically saves account credentials for the current server.")
        .defaultValue(true)
        .build()
    );

    public final Setting<String> loginCommand = sgGeneral.add(new StringSetting.Builder()
        .name("LoginCommand")
        .description("Command used to log in.")
        .defaultValue("l")
        .build()
    );

    public final Setting<String> registerCommand = sgGeneral.add(new StringSetting.Builder()
        .name("RegCommand")
        .description("Command used to register.")
        .defaultValue("reg")
        .build()
    );

    public final Setting<String> cpCommand = sgGeneral.add(new StringSetting.Builder()
        .name("CpCommand")
        .description("Command used to change passwords.")
        .defaultValue("cp")
        .build()
    );

    private boolean check = false;
    private String pw = "";
    private String lastIp = "";

    public AutoLogin() {
        super(LeavesHack.CATEGORY, "AutoLogin", "Automatically logs you into the server.");
        MeteorClient.EVENT_BUS.subscribe(new StaticListener());
        INSTANCE = this;
    }

    private List<AutoLoginAccount> accounts() {
        AutoLoginAccounts accounts = AutoLoginAccounts.get();
        return accounts != null ? accounts.getAccounts() : List.of();
    }

    private class StaticListener {
        @EventHandler
        private void onGameJoined(ServerConnectBeginEvent event) {
            check = false;
            pw = "";
            lastIp = event.address.getAddress();

            for (AutoLoginAccount account : accounts()) {
                if (account.username.get().equals(mc.getSession().getUsername()) && account.serverIp.get().equals(lastIp)) {
                    check = true;
                    pw = account.password.get();
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onMessageSend(PacketEvent.Send event) {
        if (!(event.packet instanceof CommandExecutionC2SPacket packet) || !autoSave.get()) return;

        String[] args = packet.command().split(" ");
        if (args.length < 2) return;
        if (!args[0].equals(loginCommand.get()) && !args[0].equals(registerCommand.get()) && !args[0].equals(cpCommand.get())) return;

        String password = args[0].equals(cpCommand.get()) ? (args.length > 2 ? args[2] : "") : args[1];
        if (password.isBlank()) return;

        AutoLoginAccounts accountsSystem = AutoLoginAccounts.get();
        if (accountsSystem == null) return;

        String username = mc.getSession().getUsername();
        String server = lastIp;

        for (AutoLoginAccount account : accountsSystem.getAccounts()) {
            if (account.username.get().equals(username) && account.serverIp.get().equals(server)) {
                account.password.set(password);
                accountsSystem.saveNow();
                return;
            }
        }

        AutoLoginAccount account = new AutoLoginAccount();
        account.username.set(username);
        account.serverIp.set(server);
        account.password.set(password);
        accountsSystem.add(account);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!check || mc.getNetworkHandler() == null || pw.isBlank()) return;

        mc.getNetworkHandler().sendCommand(loginCommand.get() + " " + pw);
        check = false;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        initTable(theme, table);
        return table;
    }

    private void initTable(GuiTheme theme, WTable table) {
        table.clear();
        table.add(theme.label("Username"));
        table.add(theme.label("Server"));
        table.add(theme.label("Password"));
        table.row();

        AutoLoginAccounts accountsSystem = AutoLoginAccounts.get();
        if (accountsSystem != null) {
            for (AutoLoginAccount account : accountsSystem.getAccounts()) {
                table.add(theme.label(account.username.get()));
                table.add(theme.label(account.serverIp.get() + "  "));
                table.add(theme.label(account.password.get()));

                WButton edit = table.add(theme.button("Edit")).widget();
                edit.action = () -> mc.setScreen(new EditAccountScreen(theme, account, () -> initTable(theme, table)));

                WMinus remove = table.add(theme.minus()).widget();
                remove.action = () -> {
                    accountsSystem.remove(account);
                    initTable(theme, table);
                };
                table.row();
            }
        }

        table.row();
        table.add(theme.horizontalSeparator()).expandX();
        table.row();

        WButton createAccount = table.add(theme.button("CreateAccount")).expandX().widget();
        createAccount.action = () -> mc.setScreen(new EditAccountScreen(theme, null, () -> initTable(theme, table)));
    }

    private class EditAccountScreen extends EditSystemScreen<AutoLoginAccount> {
        public EditAccountScreen(GuiTheme theme, AutoLoginAccount value, Runnable reload) {
            super(theme, value, reload);

            if (value == null) {
                this.value.username.set(mc.getSession().getUsername());
                this.value.serverIp.set(lastIp);
            }
        }

        @Override
        public AutoLoginAccount create() {
            return new AutoLoginAccount();
        }

        @Override
        public boolean save() {
            if (value.username.get().isBlank()) return false;

            AutoLoginAccounts accountsSystem = AutoLoginAccounts.get();
            if (accountsSystem == null) return false;

            if (!accounts().contains(value)) accountsSystem.add(value);
            else accountsSystem.saveNow();

            return true;
        }

        @Override
        public Settings getSettings() {
            return value.settings;
        }
    }
}
